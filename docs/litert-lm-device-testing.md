# Gemma CPU acceptance

Reference inspected: https://github.com/akeshridev/on-device-rag-android/blob/main/app/src/main/java/com/ashish/vaultmind/ml/LlmService.kt

Johar uses the same Gemma 3 1B int4 filename, CPU backend, 2048-token context, 512-token output cap, sampler and repetition controls. SDK stays at 0.17.0. Johar retrieval, evidence prompt, and frozen evaluation are unchanged. One engine per ViewModel; each request closes its own conversation.

## Build and tests

```sh
/Users/ashish/.gradle/wrapper/dists/gradle-9.3.1-bin/23ovyewtku6u96viwx3xl3oks/gradle-9.3.1/bin/gradle :johar-data:testDebugUnitTest --tests '*LiteRtLmRuntimePolicyTest' --tests '*NativeInferenceWorkerTest' :app:assembleDebug :app:lintDebug
```

## Physical ARM64 phone

Set Android SDK platform-tools on PATH. Use a complete Gemma 3 1B int4 `.litertlm` artifact; do not rename the old Qwen file. Verify its size/checksum against the artifact provider before provisioning. No Gemma artifact size/hash was supplied by the reference, so Johar only checks readability and container header at inference. Explicit downloads require a URL and expected byte count; inference never downloads.

```sh
PHONE_SERIAL='replace-with-phone-serial'
MODEL_FILE='/absolute/path/to/gemma3-1b-it-int4.litertlm'
adb -s "$PHONE_SERIAL" install -r app/build/outputs/apk/debug/app-debug.apk
adb -s "$PHONE_SERIAL" shell am force-stop com.akeshridev.johar
adb -s "$PHONE_SERIAL" shell run-as com.akeshridev.johar mkdir -p files/models
adb -s "$PHONE_SERIAL" shell "run-as com.akeshridev.johar sh -c 'cat > files/models/gemma3-1b-it-int4.litertlm.incoming'" < "$MODEL_FILE"
shasum -a 256 "$MODEL_FILE"
adb -s "$PHONE_SERIAL" shell run-as com.akeshridev.johar sha256sum files/models/gemma3-1b-it-int4.litertlm.incoming
```

Confirm hashes match, then:

```sh
adb -s "$PHONE_SERIAL" shell run-as com.akeshridev.johar mv files/models/gemma3-1b-it-int4.litertlm.incoming files/models/gemma3-1b-it-int4.litertlm
adb -s "$PHONE_SERIAL" shell am start -n com.akeshridev.johar/.MainActivity
adb -s "$PHONE_SERIAL" logcat -v threadtime 'JoharLLM:V' '*:S'
```

Enable airplane mode; tap **Test On-Device LLM → Logcat** twice. Expect `MODEL_READY`, create/initialize stages with `backend=CPU`, then conversation/generation stages. The second request must not initialize another engine. Inspect answers against retrieved evidence. Finish the activity: expect `ENGINE_CLOSE_COMPLETE`.

The 90-second timeout includes queueing, initialization, and generation. It returns to the caller even if JNI is blocked, retires this synthesizer, and queues cleanup behind the native call. It does not terminate JNI. Reopen the activity after cleanup to retry; force-stop if JNI never returns. Tests simulate blocked work and verify timeout/cleanup ordering. Emulator inference remains explicitly skipped; no phone or native Gemma runtime success is claimed.
