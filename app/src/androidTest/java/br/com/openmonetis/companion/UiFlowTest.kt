package br.com.openmonetis.companion

import android.graphics.Bitmap
import androidx.room.Room
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import br.com.openmonetis.companion.data.local.AppDatabase
import br.com.openmonetis.companion.data.local.entities.NotificationEntity
import br.com.openmonetis.companion.data.local.entities.SyncStatus
import br.com.openmonetis.companion.data.local.entities.AppConfigEntity
import br.com.openmonetis.companion.ui.notifications.NotificationFilter
import androidx.core.view.WindowCompat
import android.content.res.Configuration
import br.com.openmonetis.companion.ui.MainActivity
import br.com.openmonetis.companion.util.SecureStorage
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.assertTrue
import org.junit.Assert.assertEquals
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class UiFlowTest {
    @get:Rule val compose = createEmptyComposeRule()
    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()
    private val context get() = instrumentation.targetContext
    private var scenario: ActivityScenario<MainActivity>? = null
    @Before fun reset() {
        SecureStorage(context).clear()
        // Keep the file valid for the application-scoped Room instance between scenarios.
        val db = Room.databaseBuilder(context, AppDatabase::class.java, "openmonetis_companion.db")
            .addMigrations(AppDatabase.MIGRATION_1_2).build()
        try { db.clearAllTables() } finally { db.close() }
    }
    @After fun close() { scenario?.close(); SecureStorage(context).clear() }
    private fun capture(name: String) {
        compose.waitForIdle()
        // Give Android's compositor a frame after Compose settles.
        instrumentation.waitForIdleSync()
        Thread.sleep(350)
        val bitmap = instrumentation.uiAutomation.takeScreenshot()
        val file = File(context.getExternalFilesDir(null), "ux/$name.png")
        file.parentFile!!.mkdirs()
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }
    @Test fun setupCanBeCompletedFromKeyboardAndSurvivesRecreation() {
        scenario = ActivityScenario.launch(MainActivity::class.java)
        compose.onNodeWithText("1 de 2 · Servidor").assertIsDisplayed()
        scenario!!.onActivity { activity ->
            val light = activity.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK != Configuration.UI_MODE_NIGHT_YES
            assertEquals(light, WindowCompat.getInsetsController(activity.window, activity.window.decorView).isAppearanceLightStatusBars)
        }
        compose.onNode(hasSetTextAction()).performTextInput("https://example.invalid")
        capture("setup")
        scenario!!.recreate()
        compose.onNodeWithText("https://example.invalid").assertExists()
        compose.onNode(hasSetTextAction()).performImeAction()
        compose.waitUntil(25_000) { compose.onAllNodesWithText("Não foi possível conectar ao servidor").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Não foi possível conectar ao servidor").assertExists()
    }
    @Test fun homeHistoryErrorsDiscardUndoAndSettingsStayConsistent() = runBlocking {
        SecureStorage(context).saveCredentials("https://example.invalid", "test-token", null, "Dispositivo de teste")
        val db = Room.databaseBuilder(context, AppDatabase::class.java, "openmonetis_companion.db").addMigrations(AppDatabase.MIGRATION_1_2).build()
        val now = System.currentTimeMillis()
        try {
            db.notificationDao().insert(NotificationEntity("pending", "test.app", "Banco de teste", "Compra aprovada", "Compra de teste · R$ 1.234,56", now, "Mercado de teste", 1234.56, null, null))
            db.notificationDao().insert(NotificationEntity("failed", "test.app", "Banco de teste", null, "Compra com erro", now, null, 42.0, null, null, syncStatus = SyncStatus.SYNC_FAILED, syncError = "Token inválido ou expirado"))
        } finally { db.close() }
        scenario = ActivityScenario.launch(MainActivity::class.java)
        compose.waitUntil(10_000) { compose.onAllNodesWithText("Seu OpenMonetis").fetchSemanticsNodes().isNotEmpty() }
        capture("home")
        compose.onNode(hasScrollToNodeAction()).performScrollToNode(hasText("Ver histórico completo"))
        compose.onNodeWithText("Ver histórico completo").performClick()
        compose.onNodeWithText("Histórico de notificações").assertIsDisplayed()
        val rootBounds = compose.onRoot().fetchSemanticsNode().boundsInRoot
        NotificationFilter.entries.forEach { filter ->
            val node = compose.onNodeWithText(filter.label).assertIsDisplayed().fetchSemanticsNode()
            assertTrue("Filtro ${filter.label} ultrapassa a tela", node.boundsInRoot.left >= rootBounds.left && node.boundsInRoot.right <= rootBounds.right)
        }
        compose.onNodeWithText("Mercado de teste").assertExists()
        capture("history")
        compose.onNodeWithText("Mercado de teste").performClick()
        compose.onNodeWithText("Descartar").performScrollTo().performClick()
        compose.waitUntil(10_000) { compose.onAllNodesWithText("Desfazer").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Desfazer").performClick()
        compose.onNodeWithText("Mercado de teste").assertExists()
        compose.onNodeWithText("Com erro").performClick()
        compose.waitUntil(10_000) {
            compose.onAllNodes(hasText("Com erro") and isSelected()).fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNode(hasScrollToNodeAction()).performScrollToNode(hasText("Compra com erro"))
        compose.onNodeWithText("Compra com erro").performClick()
        compose.onNodeWithText("Atualizar token").performScrollTo().performClick()
        compose.onNodeWithText("Novo token (opcional)").assertExists()
        capture("connection")
        compose.onNodeWithText("Cancelar").performClick()
        compose.onNodeWithText("Apps Monitorados").assertExists()
        capture("settings")
        compose.onNodeWithText("Adicionar").performClick()
        compose.onNodeWithText("Buscar app").performTextInput("Chrome")
        compose.onNodeWithText("Buscar app").performImeAction()
        val chromeRow = hasText("Chrome") and !hasSetTextAction() and hasClickAction()
        compose.waitUntil(10_000) { compose.onAllNodes(chromeRow).fetchSemanticsNodes().isNotEmpty() }
        compose.onNode(chromeRow).performClick()
        capture("app-selector")
        compose.onNodeWithText("Monitorar 1 app").performClick()
        val chromeLabel = hasText("Chrome") and !hasSetTextAction()
        compose.waitUntil(10_000) {
            compose.onAllNodesWithText("Monitorar 1 app").fetchSemanticsNodes().isEmpty() &&
                compose.onAllNodes(chromeLabel).fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNode(chromeLabel).assertExists()
        Unit
    }

    @Test fun selectedAppIconIsVisibleEvenBeforeCapturePermission() = runBlocking<Unit> {
        SecureStorage(context).saveCredentials("https://example.invalid", "test-token", null, "Dispositivo de teste")
        val db = Room.databaseBuilder(context, AppDatabase::class.java, "openmonetis_companion.db")
            .addMigrations(AppDatabase.MIGRATION_1_2).build()
        try {
            db.appConfigDao().insert(AppConfigEntity(context.packageName, "Companion de teste"))
        } finally { db.close() }
        scenario = ActivityScenario.launch(MainActivity::class.java)
        compose.waitUntil(10_000) {
            compose.onAllNodesWithText("Seu OpenMonetis").fetchSemanticsNodes().isNotEmpty() &&
                compose.onAllNodesWithText("Selecionar apps").fetchSemanticsNodes().isEmpty()
        }
        compose.onNode(hasScrollToNodeAction()).performScrollToNode(hasTestTag("monitored-apps"))
        compose.onNodeWithText("Companion de teste").performScrollTo()
        capture("home-apps")
        val iconBounds = compose.onNodeWithTag("monitored-app-icon:${context.packageName}", useUnmergedTree = true)
            .assertExists().fetchSemanticsNode().boundsInRoot
        val rootBounds = compose.onRoot().fetchSemanticsNode().boundsInRoot
        assertTrue("Ícone fora da área visível: $iconBounds / $rootBounds",
            iconBounds.width > 0f && iconBounds.height > 0f && iconBounds.left >= rootBounds.left &&
                iconBounds.right <= rootBounds.right && iconBounds.top >= rootBounds.top && iconBounds.bottom <= rootBounds.bottom)
        compose.onNodeWithText("Companion de teste").assertIsDisplayed()
    }
}
