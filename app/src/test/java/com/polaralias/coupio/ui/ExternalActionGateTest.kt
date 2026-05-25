package com.polaralias.coupio.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ExternalActionGateTest {
    @Test
    fun `import file reports missing picker when action is unsupported`() {
        val gate = ExternalActionGate { action ->
            action != ExternalAction.IMPORT_FILE
        }

        assertEquals(
            "No file picker available on this device.",
            gate.blockedReason(ExternalAction.IMPORT_FILE),
        )
    }

    @Test
    fun `camera capture reports missing camera when action is unsupported`() {
        val gate = ExternalActionGate { action ->
            action != ExternalAction.CAMERA_CAPTURE
        }

        assertEquals(
            "No camera app available on this device.",
            gate.blockedReason(ExternalAction.CAMERA_CAPTURE),
        )
    }

    @Test
    fun `supported actions return no blocked reason`() {
        val gate = ExternalActionGate { true }

        assertNull(gate.blockedReason(ExternalAction.IMPORT_FILE))
        assertNull(gate.blockedReason(ExternalAction.CAMERA_CAPTURE))
    }
}
