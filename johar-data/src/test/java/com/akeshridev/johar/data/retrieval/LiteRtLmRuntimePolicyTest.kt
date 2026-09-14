package com.akeshridev.johar.data.retrieval

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LiteRtLmRuntimePolicyTest {
    @Test fun emulatorIsSeparateFromPhysicalSnapdragon() {
        assertTrue(LiteRtLmRuntimePolicy.isEmulator("google/sdk_gphone16k_arm64", "sdk_gphone16k_arm64", "ranchu", "sdk_gphone16k_arm64"))
        assertFalse(LiteRtLmRuntimePolicy.isEmulator("POCO/peridot_global/peridot:14/release-keys", "24069PC21G", "qcom", "peridot_global"))
    }

    @Test fun onlyRecognizableGpuFailuresQualifyForFallback() {
        assertEquals("GPU_UNSUPPORTED_OR_DELEGATE_FAILURE", LiteRtLmRuntimePolicy.failureKind(IllegalStateException("Failed to modify graph with delegate")))
        assertEquals("GPU_UNSUPPORTED_OR_DELEGATE_FAILURE", LiteRtLmRuntimePolicy.failureKind(IllegalStateException("initialize failed", RuntimeException("Can not find OpenCL library on this device"))))
        assertEquals("MODEL_INVALID_OR_INCOMPATIBLE", LiteRtLmRuntimePolicy.failureKind(IllegalStateException("TF_LITE_PREFILL_DECODE not found in the model; GPU delegate failed")))
        assertEquals("RUNTIME_FAILURE", LiteRtLmRuntimePolicy.failureKind(IllegalStateException("Failed to initialize engine")))
        assertEquals("RUNTIME_UNAVAILABLE", LiteRtLmRuntimePolicy.failureKind(UnsatisfiedLinkError("missing JNI library")))
    }

    @Test fun validationRejectsMissingTruncatedAndWrongFormatFiles() {
        val file = File.createTempFile("johar-model", ".litertlm")
        try {
            file.writeText("LITERTLM")
            LocalModelValidator.validate(file, 8)
            assertFailsWith<IllegalStateException> { LocalModelValidator.validate(file, 100) }
            file.writeText("LITER")
            assertFailsWith<IllegalStateException> { LocalModelValidator.validate(file) }
            file.writeText("NOTMODEL")
            assertFailsWith<IllegalStateException> { LocalModelValidator.validate(file, 8) }
            file.delete()
            assertFailsWith<IllegalStateException> { LocalModelValidator.validate(file, 8) }
        } finally { file.delete() }
    }
}
