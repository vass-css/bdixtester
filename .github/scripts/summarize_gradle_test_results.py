#!/usr/bin/env python3
"""Summarize Gradle JUnit XML into deterministic per-test and benchmark CSV files."""

from __future__ import annotations

import argparse
import csv
import math
import re
import sys
import xml.etree.ElementTree as ET
from collections import Counter
from pathlib import Path

TIMING_FIELDS = (
    "test_class",
    "test_method",
    "duration_seconds",
    "status",
    "failure_information",
)
METRIC_FIELDS = ("test_class", "metric", "value")
TEST_ANNOTATION = re.compile(r"@Test\b")
CLASS_DECLARATION = re.compile(
    r"(?m)^[ \t]*(?:(?:public|internal|private|protected|open|abstract|sealed|data|value|enum|annotation|expect|actual)\s+)*class\s+([A-Za-z_][A-Za-z0-9_]*)\b"
)
FUNCTION_DECLARATION = re.compile(
    r"^(?:(?:public|internal|private|protected|suspend|inline|tailrec|operator|infix|external|override|actual|expect|open|final)\s+)*fun\s+(`[^`]+`|[A-Za-z_][A-Za-z0-9_]*)\s*\("
)
BENCHMARK_METRIC = re.compile(r"\[PERF_BENCH\]\s*([^=\s]+)\s*=\s*(.*)")


def test_methods(source: str) -> list[str]:
    methods: list[str] = []
    waiting_for_function = False

    for line in source.splitlines():
        stripped = line.strip()
        if not waiting_for_function:
            annotation = TEST_ANNOTATION.match(stripped)
            if annotation is None:
                continue
            remainder = stripped[annotation.end() :].strip()
            function = FUNCTION_DECLARATION.match(remainder)
            if function is not None:
                methods.append(function.group(1).strip("`"))
            else:
                waiting_for_function = True
            continue

        if not stripped or stripped.startswith("//") or stripped.startswith("@"):
            continue

        function = FUNCTION_DECLARATION.match(stripped)
        if function is not None:
            methods.append(function.group(1).strip("`"))
        waiting_for_function = False

    return methods


def expected_tests(
    sources_dir: Path,
    class_filters: list[str],
) -> tuple[Counter[tuple[str, str]], list[str]]:
    expected: Counter[tuple[str, str]] = Counter()
    errors: list[str] = []
    discovered_classes: set[str] = set()

    for source_path in sorted(sources_dir.rglob("*Test.kt")):
        source = source_path.read_text(encoding="utf-8")
        package_match = re.search(r"(?m)^\s*package\s+([A-Za-z0-9_.]+)\s*$", source)
        class_match = CLASS_DECLARATION.search(source)
        if class_match is None:
            errors.append(f"{source_path}: could not identify test class")
            continue

        class_name = class_match.group(1)
        test_class = (
            f"{package_match.group(1)}.{class_name}"
            if package_match
            else class_name
        )
        methods = test_methods(source)
        if methods:
            discovered_classes.add(test_class)
            for method in methods:
                expected[(test_class, method)] += 1

    if class_filters:
        unknown_filters = sorted(set(class_filters) - discovered_classes)
        if unknown_filters:
            errors.append(
                "Test class filter did not match source inventory: "
                + ", ".join(unknown_filters)
            )
        expected = Counter(
            {
                test: count
                for test, count in expected.items()
                if test[0] in class_filters
            }
        )
    elif not expected:
        errors.append(f"No @Test methods found under {sources_dir}")

    return expected, errors


def failure_information(testcase: ET.Element) -> tuple[str, str | None]:
    failures: list[str] = []
    status: str | None = None

    for child in testcase:
        if child.tag in ("failure", "error"):
            status = "FAILED"
            parts = [child.tag]
            for attribute in ("type", "message"):
                value = child.get(attribute)
                if value:
                    parts.append(f"{attribute}: {value}")
            body = "".join(child.itertext()).strip()
            if body:
                parts.append(body)
            failures.append(" | ".join(parts))
        elif child.tag == "skipped":
            status = "SKIPPED"
            message = child.get("message") or "".join(child.itertext()).strip()
            failures.append("skipped" + (f": {message}" if message else ""))

    return " ".join(failures), status


def normalized_duration(raw_duration: str | None) -> tuple[str, str | None]:
    if raw_duration is None:
        return "", "Missing testcase time attribute"
    try:
        duration = float(raw_duration)
    except ValueError:
        return "", f"Invalid testcase time attribute: {raw_duration!r}"
    if not math.isfinite(duration) or duration < 0:
        return "", f"Invalid testcase time attribute: {raw_duration!r}"
    return format(duration, ".6f"), None


def parse_xml_results(
    results_dir: Path,
    expected: Counter[tuple[str, str]],
) -> tuple[list[dict[str, str]], list[dict[str, str]], Counter[tuple[str, str]]]:
    rows: list[dict[str, str]] = []
    metrics: list[dict[str, str]] = []
    observed: Counter[tuple[str, str]] = Counter()

    xml_paths = sorted(results_dir.glob("TEST-*.xml")) if results_dir.exists() else []
    for xml_path in xml_paths:
        try:
            root = ET.parse(xml_path).getroot()
        except (ET.ParseError, OSError) as error:
            rows.append(
                {
                    "test_class": xml_path.stem.removeprefix("TEST-"),
                    "test_method": "",
                    "duration_seconds": "",
                    "status": "ERROR",
                    "failure_information": f"Could not read JUnit XML {xml_path}: {error}",
                }
            )
            continue

        for suite in root.iter("testsuite"):
            suite_name = suite.get("name", "")
            for child in suite:
                if child.tag == "system-out":
                    for line in (child.text or "").splitlines():
                        match = BENCHMARK_METRIC.search(line)
                        if match:
                            metrics.append(
                                {
                                    "test_class": suite_name,
                                    "metric": match.group(1),
                                    "value": match.group(2).strip(),
                                }
                            )
                elif child.tag in ("failure", "error"):
                    detail = " ".join("".join(child.itertext()).split())
                    rows.append(
                        {
                            "test_class": suite_name,
                            "test_method": "",
                            "duration_seconds": "",
                            "status": "FAILED",
                            "failure_information": (
                                f"JUnit suite {child.tag}: "
                                f"{child.get('message', '')} {detail}"
                            ).strip(),
                        }
                    )

        for testcase in root.iter("testcase"):
            test_class = testcase.get("classname", "")
            if not test_class:
                parent_suite = next(root.iter("testsuite"), None)
                test_class = parent_suite.get("name", "") if parent_suite is not None else ""
            test_method = testcase.get("name", "")
            key = (test_class, test_method)

            failure_info, result_status = failure_information(testcase)
            duration, duration_error = normalized_duration(testcase.get("time"))
            if duration_error:
                failure_info = "; ".join(part for part in (failure_info, duration_error) if part)
                result_status = "ERROR" if result_status in (None, "SKIPPED") else result_status
            if result_status is None:
                result_status = "PASSED"

            if expected[key] > observed[key]:
                observed[key] += 1
            else:
                unexpected = "No matching expected test method was found"
                failure_info = "; ".join(part for part in (failure_info, unexpected) if part)
                if result_status == "PASSED":
                    result_status = "UNEXPECTED"

            rows.append(
                {
                    "test_class": test_class,
                    "test_method": test_method,
                    "duration_seconds": duration,
                    "status": result_status,
                    "failure_information": failure_info,
                }
            )

    for (test_class, test_method), count in sorted(expected.items()):
        missing = count - observed[(test_class, test_method)]
        for _ in range(max(0, missing)):
            rows.append(
                {
                    "test_class": test_class,
                    "test_method": test_method,
                    "duration_seconds": "",
                    "status": "MISSING",
                    "failure_information": "No testcase result found in Gradle JUnit XML",
                }
            )

    return rows, metrics, observed


def write_csv(path: Path, fields: tuple[str, ...], rows: list[dict[str, str]]) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    with path.open("w", encoding="utf-8", newline="") as output:
        writer = csv.DictWriter(output, fieldnames=fields, lineterminator="\n")
        writer.writeheader()
        writer.writerows(
            sorted(
                rows,
                key=lambda row: tuple(row.get(field, "") for field in fields),
            )
        )


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--results-dir", type=Path, required=True)
    parser.add_argument("--sources-dir", type=Path, required=True)
    parser.add_argument("--output", type=Path, required=True)
    parser.add_argument("--metrics-output", type=Path, required=True)
    parser.add_argument(
        "--class-filter",
        action="append",
        default=[],
        help="Expected fully qualified test class; may be supplied more than once.",
    )
    arguments = parser.parse_args()

    expected, inventory_errors = expected_tests(
        arguments.sources_dir,
        arguments.class_filter,
    )
    rows, metrics, _ = parse_xml_results(arguments.results_dir, expected)
    for error in inventory_errors:
        rows.append(
            {
                "test_class": "",
                "test_method": "",
                "duration_seconds": "",
                "status": "ERROR",
                "failure_information": error,
            }
        )

    write_csv(arguments.output, TIMING_FIELDS, rows)
    write_csv(arguments.metrics_output, METRIC_FIELDS, metrics)

    status_counts = Counter(row["status"] for row in rows)
    print(
        f"Wrote {len(rows)} test timing rows to {arguments.output}; "
        f"statuses={dict(sorted(status_counts.items()))}"
    )
    print(f"Wrote {len(metrics)} benchmark metrics to {arguments.metrics_output}")

    return 0 if all(row["status"] == "PASSED" for row in rows) else 1


if __name__ == "__main__":
    sys.exit(main())
