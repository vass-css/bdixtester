# Compose Performance

## Existing Structure

The application uses a single Compose activity, `StateFlow`-based feature state, `LazyColumn`, `LazyVerticalGrid`, and stable UI state models.

## Audit Notes

- Server filtering and sorting are performed in `ServersViewModel.uiState`.
- Catalog partition and classification are maintained as derived state.
- Material theme and semantic color resources are resolved at composition boundaries.
- UI state should continue to be checked with Compose composition tracing for recomposition counts.

## Remaining Verification

Use Android Studio Layout Inspector and Compose tracing to measure:

- recompositions per server-list scroll;
- navigation frame duration;
- first compose frame;
- P95/P99 frame overrun.
