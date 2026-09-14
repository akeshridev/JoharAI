# Gemma CPU acceptance

Reference implementation: `akeshridev/on-device-rag-android` (`LlmService.kt`).

Johar currently uses Gemma 3 1B int4 with LiteRT-LM `0.17.0`, CPU backend, 2,048-token context, 512-token output cap, and the same sampler/repetition controls proven in the older on-device RAG project.

Retrieval, grounded evidence construction, and the frozen retrieval evaluation remain independent from the native model runtime.

## Current status

Real native Gemma inference is now **verified on the ARM64 Android emulator**.

Verified environment:

```text
model=sdk_gphone16k_arm64
hardware=ranchu
abis=arm64-v8a
model file=gemma3-1b-it-int4.litertlm
model bytes=584417280
backend=CPU
```

Observed first request:

```text
ENGINE_CREATE_START backend=CPU
ENGINE_CREATE_COMPLETE backend=CPU
ENGINE_INITIALIZE_START backend=CPU
ENGINE_INITIALIZE_COMPLETE backend=CPU latencyMs=2289
CONVERSATION_CREATE_START backend=CPU
GENERATION_START backend=CPU
GENERATION_COMPLETE backend=CPU latencyMs=988 answerChars=182
SUCCESS latencyMs=3287
```

Observed warm second request:

```text
CONVERSATION_CREATE_START backend=CPU
GENERATION_START backend=CPU
GENERATION_COMPLETE backend=CPU latencyMs=1024 answerChars=182
SUCCESS latencyMs=1027
```

There was no second `ENGINE_CREATE_START` or `ENGINE_INITIALIZE_START`, confirming engine reuse.

Physical Android phone performance remains unverified.

## Runtime behavior

- One `LiteRtLmAnswerSynthesizer` is owned by the ViewModel lifecycle.
- One initialized LiteRT-LM `Engine` is retained and reused.
- Each request creates and closes a fresh `Conversation`.
- Native operations are serialized by `NativeInferenceWorker`.
- The caller timeout is 90 seconds.
- A timeout can release the coroutine caller but cannot forcibly terminate a JNI call already blocked inside native code.
- `close()` is called when the owning ViewModel clears.

## Build and tests

Targeted unit tests plus APK/lint can be run with:

```sh
/Users/ashish/.gradle/wrapper/dists/gradle-9.3.1-bin/23ovyewtku6u96viwx3xl3oks/gradle-9.3.1/bin/gradle \
  :johar-data:testDebugUnitTest \
  --tests '*LiteRtLmRuntimePolicyTest' \
  --tests '*NativeInferenceWorkerTest' \
  :app:assembleDebug \
  :app:lintDebug
```

## Model provisioning for local development

Use the actual Gemma 3 1B int4 `.litertlm` artifact. Do not rename the old Qwen model and treat it as Gemma.

The development artifact currently used by Johar is:

```text
gemma3-1b-it-int4.litertlm
584417280 bytes
```

A simple development flow is:

```sh
adb push /absolute/path/gemma3-1b-it-int4.litertlm /data/local/tmp/
adb shell run-as com.akeshridev.johar mkdir -p files/models
adb shell run-as com.akeshridev.johar cp /data/local/tmp/gemma3-1b-it-int4.litertlm files/models/
```

Verify the private copy:

```sh
adb shell run-as com.akeshridev.johar stat -c %s files/models/gemma3-1b-it-int4.litertlm
adb shell run-as com.akeshridev.johar sha256sum files/models/gemma3-1b-it-int4.litertlm
```

Compare its SHA-256 with the source file on the development machine.

After copying, the expected private path is:

```text
/data/user/0/com.akeshridev.johar/files/models/gemma3-1b-it-int4.litertlm
```

The model payload must stay outside Git and outside the APK/assets.

## Emulator acceptance test

1. Build/install the current debug APK.
2. Provision the Gemma model into app-private storage.
3. Start Johar.
4. Filter Logcat by `JoharLLM`.
5. Tap **Test On-Device LLM → Logcat**.
6. Tap it a second time.

Expected first-run stages:

```text
DEVICE
MODEL_READY
ENGINE_CREATE_START
ENGINE_CREATE_COMPLETE
ENGINE_INITIALIZE_START
ENGINE_INITIALIZE_COMPLETE
CONVERSATION_CREATE_START
GENERATION_START
GENERATION_COMPLETE
SUCCESS
```

Expected second-run stages:

```text
DEVICE
CONVERSATION_CREATE_START
GENERATION_START
GENERATION_COMPLETE
SUCCESS
```

The second run should not create or initialize another engine.

## Physical ARM64 phone acceptance test

A physical phone is still required before making production performance claims.

```sh
PHONE_SERIAL='replace-with-phone-serial'
MODEL_FILE='/absolute/path/gemma3-1b-it-int4.litertlm'

adb -s "$PHONE_SERIAL" install -r app/build/outputs/apk/debug/app-debug.apk
adb -s "$PHONE_SERIAL" shell am force-stop com.akeshridev.johar
adb -s "$PHONE_SERIAL" push "$MODEL_FILE" /data/local/tmp/gemma3-1b-it-int4.litertlm
adb -s "$PHONE_SERIAL" shell run-as com.akeshridev.johar mkdir -p files/models
adb -s "$PHONE_SERIAL" shell run-as com.akeshridev.johar cp /data/local/tmp/gemma3-1b-it-int4.litertlm files/models/
adb -s "$PHONE_SERIAL" shell run-as com.akeshridev.johar sha256sum files/models/gemma3-1b-it-int4.litertlm
adb -s "$PHONE_SERIAL" shell am start -n com.akeshridev.johar/.MainActivity
adb -s "$PHONE_SERIAL" logcat -v threadtime 'JoharLLM:V' '*:S'
```

Tap the LLM test twice and record:

- model validation result
- initialization latency
- first-generation latency
- warm-generation latency
- memory behavior
- thermal behavior for repeated requests
- answer quality

Do not extrapolate phone performance from the emulator.

## Current reference query

```text
Rugra Jharkhand me special kyun hai?
```

Verified generated answer:

```text
Rugra Jharkhand me special kyun hai. Rugra as an indigenous seasonal food of Jharkhand-is unique due to its traditional preparation methods and ingredients used by local communities!
```

The runtime result is successful, but the sentence quality is not yet ideal. In particular, question echo and awkward punctuation/wording are answer-quality issues to track in the model test matrix rather than runtime failures.

See `docs/model-answer-test-cases.md` for the tuning/evaluation suite.
