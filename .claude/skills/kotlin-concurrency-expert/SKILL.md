Kotlin Concurrency Expert
Overview
Review and fix Kotlin Coroutines issues in Android codebases by applying structured concurrency, lifecycle safety, proper scoping, and modern best practices with minimal behavior changes.

Workflow
1. Triage the Issue
Capture the exact error, crash, or symptom (ANR, memory leak, race condition, incorrect state).
Check project coroutines setup: kotlinx-coroutines-android version, lifecycle-runtime-ktx version.
Identify the current scope context (viewModelScope, lifecycleScope, custom scope, or none).
Confirm whether the code is UI-bound (Dispatchers.Main) or intended to run off the main thread (Dispatchers.IO, Dispatchers.Default).
Verify Dispatcher injection patterns for testability.
2. Apply the Smallest Safe Fix
Prefer edits that preserve existing behavior while satisfying structured concurrency and lifecycle safety.

Common fixes:

ANR / Main thread blocking: Move heavy work to withContext(Dispatchers.IO) or Dispatchers.Default; ensure suspend functions are main-safe.
Memory leaks / zombie coroutines: Replace GlobalScope with a lifecycle-bound scope (viewModelScope, lifecycleScope, or injected applicationScope).
Lifecycle collection issues: Replace deprecated launchWhenStarted with repeatOnLifecycle(Lifecycle.State.STARTED).
State exposure: Encapsulate MutableStateFlow / MutableSharedFlow; expose read-only StateFlow or Flow.
CancellationException swallowing: Ensure generic catch (e: Exception) blocks rethrow CancellationException.
Non-cooperative cancellation: Add ensureActive() or yield() in tight loops for cooperative cancellation.
Callback APIs: Convert listeners to callbackFlow with proper awaitClose cleanup.
Hardcoded Dispatchers: Inject CoroutineDispatcher via constructor for testability.
  
