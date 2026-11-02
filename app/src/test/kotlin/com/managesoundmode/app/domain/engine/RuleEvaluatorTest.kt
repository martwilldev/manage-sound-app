package com.managesoundmode.app.domain.engine

import com.managesoundmode.app.domain.model.LocationRule
import com.managesoundmode.app.domain.model.RestorePolicy
import com.managesoundmode.app.domain.model.ScheduleRule
import com.managesoundmode.app.domain.model.SoundMode
import com.managesoundmode.app.domain.model.SoundProfile
import com.managesoundmode.app.domain.model.TimedModeSession
import com.managesoundmode.app.domain.model.VolumeStreams
import com.managesoundmode.app.domain.model.Weekday
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RuleEvaluatorTest {

  private val profiles = SoundProfile.defaultPresets

  @Test
  fun isScheduleRuleActiveAt_handlesMidnightCrossing_eveningAndEarlyMorning() {
    // Regra Dormir: 22:30 às 07:00 (Dom a Qui)
    val sleepRule = ScheduleRule(
      id = "sleep",
      name = "Dormir",
      startHour = 22,
      startMinute = 30,
      endHour = 7,
      endMinute = 0,
      daysOfWeek = Weekday.sleepWeeknights, // SUN, MON, TUE, WED, THU
      targetProfileId = "preset_sleep",
    )

    // Domingo 2026-11-01 às 23:15 -> Ativo (iniciou no Domingo)
    val sundayNight = LocalDateTime.of(2026, 11, 1, 23, 15)
    assertTrue(RuleEvaluator.isScheduleRuleActiveAt(sleepRule, sundayNight))

    // Segunda-feira 2026-11-02 às 06:30 -> Ativo (madrugada da noite de Domingo)
    val mondayEarlyMorning = LocalDateTime.of(2026, 11, 2, 6, 30)
    assertTrue(RuleEvaluator.isScheduleRuleActiveAt(sleepRule, mondayEarlyMorning))

    // Segunda-feira 2026-11-02 às 07:00 -> Inativo (fim exato da janela)
    val mondaySevenAm = LocalDateTime.of(2026, 11, 2, 7, 0)
    assertFalse(RuleEvaluator.isScheduleRuleActiveAt(sleepRule, mondaySevenAm))

    // Sexta-feira 2026-11-06 às 23:15 -> Inativo (Sexta não está em sleepWeeknights)
    val fridayNight = LocalDateTime.of(2026, 11, 6, 23, 15)
    assertFalse(RuleEvaluator.isScheduleRuleActiveAt(sleepRule, fridayNight))

    // Sábado 2026-11-07 às 02:00 -> Inativo (noite anterior foi Sexta)
    val saturdayEarlyMorning = LocalDateTime.of(2026, 11, 7, 2, 0)
    assertFalse(RuleEvaluator.isScheduleRuleActiveAt(sleepRule, saturdayEarlyMorning))
  }

  @Test
  fun evaluate_conflictBetweenScheduleAndLocation_mostRestrictiveWinsByDefault() {
    // Segunda-feira 10:00: Horário Comercial (Normal/Alto) vs. Local Faculdade (Vibrar)
    val workSchedule = ScheduleRule(
      id = "work",
      name = "Trabalho Alto",
      startHour = 8,
      startMinute = 0,
      endHour = 18,
      endMinute = 0,
      daysOfWeek = Weekday.weekdays,
      targetProfileId = "preset_work_high", // NORMAL
      enabled = true,
      highPriority = false,
    )
    val silentLocation = LocationRule(
      id = "clinic",
      name = "Clínica Médica",
      latitude = 0.0,
      longitude = 0.0,
      targetProfileId = "preset_work", // VIBRATE
      enabled = true,
      isCurrentlyInside = true,
      highPriority = false,
    )

    val mondayTenAm = LocalDateTime.of(2026, 11, 2, 10, 0)
    val decision = RuleEvaluator.evaluate(
      nowDateTime = mondayTenAm,
      nowEpochMillis = 1_000_000L,
      profiles = profiles,
      scheduleRules = listOf(workSchedule),
      locationRules = listOf(silentLocation),
      activeTimer = null,
      overriddenSessionKey = null,
      restorePolicy = RestorePolicy.DEFAULT_PROFILE,
    )

    assertTrue(decision is RuleEvaluationDecision.ApplyRule)
    val apply = decision as RuleEvaluationDecision.ApplyRule
    assertEquals("clinic", apply.winner.ruleId)
    assertEquals(SoundMode.VIBRATE, apply.winner.profile.mode)
  }

  @Test
  fun evaluate_highPriorityRule_overridesMoreRestrictiveRule() {
    // Horário de Plantão (NORMAL com highPriority = true) vence Local Silencioso (VIBRATE)
    val onCallSchedule = ScheduleRule(
      id = "on_call",
      name = "Plantão Médico",
      startHour = 8,
      startMinute = 0,
      endHour = 18,
      endMinute = 0,
      daysOfWeek = Weekday.weekdays,
      targetProfileId = "preset_work_high", // NORMAL 100%
      enabled = true,
      highPriority = true,
    )
    val vibrateLocation = LocationRule(
      id = "cinema",
      name = "Shopping",
      latitude = 0.0,
      longitude = 0.0,
      targetProfileId = "preset_work", // VIBRATE
      enabled = true,
      isCurrentlyInside = true,
      highPriority = false,
    )

    val mondayTenAm = LocalDateTime.of(2026, 11, 2, 10, 0)
    val decision = RuleEvaluator.evaluate(
      nowDateTime = mondayTenAm,
      nowEpochMillis = 1_000_000L,
      profiles = profiles,
      scheduleRules = listOf(onCallSchedule),
      locationRules = listOf(vibrateLocation),
      activeTimer = null,
      overriddenSessionKey = null,
      restorePolicy = RestorePolicy.DEFAULT_PROFILE,
    )

    assertTrue(decision is RuleEvaluationDecision.ApplyRule)
    val apply = decision as RuleEvaluationDecision.ApplyRule
    assertEquals("on_call", apply.winner.ruleId)
    assertEquals(SoundMode.NORMAL, apply.winner.profile.mode)
  }

  @Test
  fun evaluate_whenTimerIsActive_timerBeatsAllRules() {
    val sleepRule = ScheduleRule.defaultSchedules.first()
    val timer = TimedModeSession(
      targetMode = SoundMode.VIBRATE,
      previousMode = SoundMode.NORMAL,
      previousStreams = VolumeStreams(),
      durationMinutes = 30,
      startedAtEpochMillis = 1_000L,
      endsAtEpochMillis = 1_801_000L,
    )

    val sundayNight = LocalDateTime.of(2026, 11, 1, 23, 0)
    val decision = RuleEvaluator.evaluate(
      nowDateTime = sundayNight,
      nowEpochMillis = 600_000L,
      profiles = profiles,
      scheduleRules = listOf(sleepRule),
      locationRules = emptyList(),
      activeTimer = timer,
      overriddenSessionKey = null,
      restorePolicy = RestorePolicy.DEFAULT_PROFILE,
    )

    assertTrue(decision is RuleEvaluationDecision.TimerActive)
  }

  @Test
  fun evaluate_whenSessionKeyMatchesManualOverride_returnsRuleOverriddenByUser() {
    val workSchedule = ScheduleRule(
      id = "sched_work",
      name = "Trabalho",
      startHour = 8,
      startMinute = 30,
      endHour = 18,
      endMinute = 0,
      daysOfWeek = Weekday.weekdays,
      targetProfileId = "preset_work_high",
      enabled = true,
    )
    val mondayTenAm = LocalDateTime.of(2026, 11, 2, 10, 0)

    val firstEval = RuleEvaluator.evaluate(
      nowDateTime = mondayTenAm,
      nowEpochMillis = 1_000L,
      profiles = profiles,
      scheduleRules = listOf(workSchedule),
      locationRules = emptyList(),
      activeTimer = null,
      overriddenSessionKey = null,
      restorePolicy = RestorePolicy.DEFAULT_PROFILE,
    ) as RuleEvaluationDecision.ApplyRule

    val overriddenEval = RuleEvaluator.evaluate(
      nowDateTime = mondayTenAm,
      nowEpochMillis = 2_000L,
      profiles = profiles,
      scheduleRules = listOf(workSchedule),
      locationRules = emptyList(),
      activeTimer = null,
      overriddenSessionKey = firstEval.winner.sessionKey,
      restorePolicy = RestorePolicy.DEFAULT_PROFILE,
    )

    assertTrue(overriddenEval is RuleEvaluationDecision.RuleOverriddenByUser)
  }

  @Test
  fun computeNextScheduleTransitionMillis_returnsUpcomingBoundary() {
    val now = ZonedDateTime.of(2026, 11, 2, 8, 0, 0, 0, ZoneId.of("America/Sao_Paulo"))
    val nextMillis = RuleEvaluator.computeNextScheduleTransitionMillis(
      scheduleRules = ScheduleRule.defaultSchedules,
      now = now,
    )
    assertNotNull(nextMillis)
    val expectedEightThirty = ZonedDateTime.of(2026, 11, 2, 8, 30, 0, 0, ZoneId.of("America/Sao_Paulo"))
      .toInstant()
      .toEpochMilli()
    assertEquals(expectedEightThirty, nextMillis)
  }
}
