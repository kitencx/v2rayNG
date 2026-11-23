package com.v2ray.ang.ui.routing

import com.v2ray.ang.dto.entities.RulesetItem
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class RoutingIdentityTest {
    @Test
    fun newEditorsAndLegacyLinksResolveExplicitly() {
        val rule = RulesetItem(id = "stable")
        assertNull(resolveRuleForEditor(listOf(rule), null, -1))
        assertEquals(rule, resolveRuleForEditor(listOf(rule), null, 0))
        assertEquals(rule, resolveRuleForEditor(listOf(RulesetItem(id = "other"), rule), "stable", 0))
    }

    @Test
    fun staleEditorLinksCannotSilentlyBecomeNewRules() {
        assertThrows(IllegalStateException::class.java) { resolveRuleForEditor(emptyList(), "removed", -1) }
        assertThrows(IllegalStateException::class.java) { resolveRuleForEditor(emptyList(), null, 0) }
    }

    @Test
    fun editingAfterReorderChangesTheSameRuleId() {
        val first = RulesetItem(id = "first", remarks = "first name")
        val second = RulesetItem(id = "second", remarks = "second name")
        val updated = requireNotNull(replaceRuleById(listOf(second, first), "first", first.copy(remarks = "edited")))
        assertEquals(listOf("second", "first"), updated.map { it.id })
        assertEquals("edited", updated[1].remarks)
        assertEquals(second, updated[0])
        assertEquals("first name", first.remarks)
    }

    @Test
    fun removedRulesAreNotRecreatedOrAppliedToAnotherPosition() {
        assertNull(replaceRuleById(listOf(RulesetItem(id = "other")), "removed", RulesetItem(id = "removed")))
        assertNull(replaceRuleById(emptyList(), "removed", RulesetItem()))
    }

    @Test
    fun aReplacementCannotChangeThePersistedIdentity() {
        val result = requireNotNull(replaceRuleById(listOf(RulesetItem(id = "stable")), "stable", RulesetItem(id = "wrong")))
        assertEquals("stable", result.single().id)
    }
}
