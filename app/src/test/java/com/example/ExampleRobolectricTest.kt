package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.preferences.AssistantSettings
import com.example.data.preferences.AssistantSettingsRepository
import com.example.data.repository.AssistantRepository
import com.example.domain.CommandExecutionOutcome
import com.example.domain.LocalCommandProcessor
import com.example.voice.StandardOnDeviceWakePhraseMatcher
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    private lateinit var context: Context
    private lateinit var db: AppDatabase
    private lateinit var repo: AssistantRepository

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repo = AssistantRepository(db.assistantDao())
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `verify app name resource`() {
        val appName = context.getString(R.string.app_name)
        assertEquals("Personal AI Assistant", appName)
    }

    @Test
    fun `verify assistant name changes persist without deleting chat history or attendance`() = runBlocking {
        val settingsRepo = AssistantSettingsRepository(context)
        val sessionId = repo.ensureSeededAndInitialSession()
        repo.addMessage(sessionId, "USER", "Mera hisab save rakhna")
        repo.clockInToday(biometricVerified = true, notes = "Morning shift")

        // Change assistant name to Raja
        settingsRepo.updateAssistantName("Raja")
        val updatedSettings = settingsRepo.settingsFlow.first()
        assertEquals("Raja", updatedSettings.assistantName)
        assertEquals("Hey Raja", updatedSettings.wakePhrase)

        // Verify chat messages and attendance survive assistant rename
        val messages = repo.observeMessages(sessionId).first()
        assertTrue(messages.size >= 2)
        val todayAtt = repo.getTodayAttendance()
        assertNotNull(todayAtt)
        assertEquals("PRESENT", todayAtt?.status)
    }

    @Test
    fun `verify Hindi and Hinglish custom commands and offline calculator work`() = runBlocking {
        repo.ensureSeededAndInitialSession()
        val commands = repo.customCommands.first()
        assertTrue(commands.size >= 8)

        val processor = LocalCommandProcessor(context)

        // Test "Time batao"
        val timeOutcome = processor.processInput("Time batao", "Raja", commands, null, emptyList())
        assertTrue(timeOutcome is CommandExecutionOutcome.ExecutedOffline)

        // Test "Mujhe jawab bolkar do"
        val ttsOutcome = processor.processInput("Mujhe jawab bolkar do", "Raja", commands, null, emptyList())
        assertTrue(ttsOutcome is CommandExecutionOutcome.ExecutedOffline)
        assertTrue((ttsOutcome as CommandExecutionOutcome.ExecutedOffline).enableTtsRequested)

        // Test offline calculator math
        val mathOutcome = processor.processInput("hisab karo 250 * 4", "Raja", commands, null, emptyList())
        assertTrue(mathOutcome is CommandExecutionOutcome.ExecutedOffline)
        assertTrue((mathOutcome as CommandExecutionOutcome.ExecutedOffline).spokenAndChatReply.contains("1000"))
    }

    @Test
    fun `verify wake phrase matcher and verified project zip export`() = runBlocking {
        val matcher = StandardOnDeviceWakePhraseMatcher()
        assertTrue(matcher.matchesWakePhrase("Hey Raja aaj ki attendance batao", "Hey Raja", "Raja"))
        assertTrue(matcher.matchesWakePhrase("Namaste Raja", "Hey Raja", "Raja"))

        val zipInfo = repo.generateAndVerifyProjectZip(
            context = context,
            settings = AssistantSettings(assistantName = "Raja", wakePhrase = "Hey Raja")
        )
        assertTrue(zipInfo.isValid)
        assertTrue(zipInfo.file.exists())
        assertTrue(zipInfo.sizeBytes > 100L)
        assertTrue(zipInfo.entryCount >= 4)
    }
}
