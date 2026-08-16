---
artifact_contract: ce-unified-plan/v1
artifact_readiness: implementation-ready
product_contract_source: ce-brainstorm
title: Equipment Detail View - Plan
type: feat
date: 2026-08-16
topic: equipment-detail-view
execution: code
---

# Equipment Detail View - Plan

## Goal Capsule

- **Objective:** Let a user inspect the character's current equipped items and open a dedicated detail view for each item.
- **Product authority:** This plan extends the existing anonymous Maple Growth Tracker dashboard and its current growth-insights experience.
- **Active scope:** Current active, non-cash equipment only; historical equipment comparison is not part of this work.
- **Open blockers:** None for the product scope. Planning must verify which optional Nexon fields are available per item and preserve the values-only display rule.

## Product Contract

### Summary

Add a current-equipment section to the character dashboard and a dedicated detail view for each equipped item. The detail view presents every available item attribute while hiding sections whose values are absent.

### Problem Frame

The dashboard currently exposes growth metrics and grouped equipment-replacement events, but it does not let a user inspect the equipment that produced the current state. A user who wants to understand the character's present build must leave the dashboard and consult another source.

This work makes current equipment an inspectable part of growth analysis without expanding into historical diff analysis or item recommendations.

### Key Decisions

- KTD1. **Current equipment over history:** Use the current snapshot as the source of the equipment view (session-settled: user-directed — chosen over historical comparison: keep this work focused on current-state inspection). Governs R1, R7, R10.
- KTD2. **Dedicated detail screen:** Open a separate detail view for an item rather than expanding rows inline (session-settled: user-approved — chosen over inline or panel detail: detailed item information needs more room and a stable future extension point). Governs R4, R5, R9.
- KTD3. **Values-only sections:** Render an attribute section only when that item provides a value for it (session-settled: user-directed — chosen over fixed empty sections: avoid clutter from unavailable item attributes). Governs R6, R8.
- KTD4. **Current-state-first entry point:** Make the equipment list the primary surface and keep equipment history or comparison deferred (session-settled: user-directed — chosen over change-history-first exploration: the immediate gap is visibility into the current build). Governs R2, R10.

<!-- ce-section: work-relationships -->
### How This Work Fits Together

This plan owns the current-equipment inspection slice of the broader growth-analysis expansion. The surrounding work is contextual and may be planned separately:

- Equipment history and snapshot-to-snapshot comparison can proceed independently after the current detail contract is stable.
- Equipment performance scoring or recommendations depend on product rules that this plan intentionally does not define.
- The existing growth chart and event timeline remain the surrounding dashboard context; this work adds an equipment surface without changing their meaning.

### Requirements

#### Equipment entry and list

- R1. The dashboard shall expose the character's current active equipped items from the latest available snapshot.
- R2. The list shall organize active equipment by its user-recognizable slot or equipment part and show the item name for each populated slot.
- R3. Each list entry shall provide enough identity information for the user to distinguish items before opening its detail view, including the item image when available.
- R4. Selecting a list entry shall open a dedicated detail view for that item, and returning from the detail view shall bring the user back to the equipment list.

#### Item detail

- R5. The detail view shall show the selected item's identity, slot, and current snapshot context.
- R6. The detail view shall show every supported item attribute that has a value, including available enhancement, Star Force, potential, additional-potential, scroll, base-stat, and bonus-stat information.
- R7. The detail view shall represent the current snapshot only and shall not imply that its values are historical or a comparison result.
- R8. Missing, null, or malformed optional attributes shall be omitted from the detail view without creating inferred values.

#### State and access

- R9. The equipment list and detail view shall provide loading, unavailable-data, and retryable-error states that preserve the dashboard's existing cached-data behavior where applicable.
- R10. The dashboard shall clearly distinguish an empty equipment result from a populated equipment list, and the detail view shall handle an item that is no longer available in the latest snapshot.
- R11. The list and detail view shall remain usable on mobile, support keyboard navigation, and expose item names and state changes through accessible text rather than color alone.
- R12. Public responses shall expose only the normalized item data needed by the list and detail view and shall not expose raw Nexon payloads or backend secrets.

### Key Flows

- F1. **Inspect current equipment**
  - **Trigger:** The user opens a character dashboard with a current snapshot.
  - **Steps:** The user scans the current equipment list, selects an item, reviews its available detail sections, and returns to the list.
  - **Outcome:** The user can identify the current build and inspect an individual item's present state without leaving the product.
  - **Covered by:** R1, R2, R4, R5, R6, R7.

- F2. **Handle incomplete item data**
  - **Trigger:** An item has missing optional attributes or the snapshot has no usable equipment rows.
  - **Steps:** The product keeps valid identity data visible, omits unavailable sections, and shows an explanatory empty or unavailable state when no usable item exists.
  - **Outcome:** The user sees what is known without mistaking missing data for zero values or a failed comparison.
  - **Covered by:** R8, R9, R10.

- F3. **Use the feature on mobile**
  - **Trigger:** The user opens the dashboard or an item detail view on a narrow viewport or with keyboard navigation.
  - **Steps:** The user reaches the equipment list, opens an item, reads its sections, and returns using the available navigation affordance.
  - **Outcome:** The current equipment remains inspectable without horizontal overflow or inaccessible controls.
  - **Covered by:** R4, R11.

### Acceptance Examples

- AE1. **Populated current equipment**
  - **Given:** The latest snapshot contains multiple active equipment rows.
  - **When:** The user opens the dashboard.
  - **Then:** The equipment section lists the populated slots with item names and available identity imagery.
  - **Covers:** R1, R2, R3.

- AE2. **Dedicated item detail**
  - **Given:** The equipment list contains an item with name, slot, image, enhancement, and potential values.
  - **When:** The user selects that item.
  - **Then:** A dedicated detail view shows the item identity and each available attribute group, and the user can return to the list.
  - **Covers:** R4, R5, R6, R7.

- AE3. **Values-only rendering**
  - **Given:** An item has no additional-potential value and has a null optional stat group.
  - **When:** The user opens its detail view.
  - **Then:** Those empty sections are not rendered and no zero or inferred value is displayed.
  - **Covers:** R6, R8.

- AE4. **No usable equipment**
  - **Given:** The latest snapshot contains no valid active equipment rows.
  - **When:** The user opens the dashboard.
  - **Then:** The equipment section shows a clear data-unavailable or empty state without a broken item link.
  - **Covers:** R9, R10.

- AE5. **Mobile and keyboard access**
  - **Given:** The user is on a mobile viewport or navigating with a keyboard.
  - **When:** The user moves from the equipment list to a detail view and back.
  - **Then:** The controls are reachable, the detail content fits the viewport, and the item identity remains available as text.
  - **Covers:** R4, R11.

### Scope Boundaries

#### Deferred for later

- Snapshot-to-snapshot equipment comparison and replacement history beyond the existing grouped event.
- Equipment option, Star Force, potential, additional-potential, or scroll change analysis across dates.
- Set-effect aggregation, character-level equipment scoring, upgrade recommendations, and build advice.
- Cash equipment, presets, and separate job-specific equipment collections.

#### Outside this work's identity

- Login, favorites, notifications, sharing, and community ranking features.
- Automatic judgments about whether an item is good, bad, optimal, or worth replacing.

### Dependencies and Assumptions

- The latest successful snapshot is the authoritative source for the current equipment view.
- Optional item attributes may differ by item type and must remain unknown when absent.
- The item detail experience must use the project's existing API secret boundary: Nexon access stays in the backend and raw payloads stay private.
- The existing dashboard's cached-data, loading, error, mobile, and accessibility conventions remain the baseline for this feature.

### Product Contract Preservation

Product Contract unchanged.

## Planning Contract

### Key Technical Decisions

- KTD5. **Normalize before public exposure:** Convert the stored raw equipment payload into a narrow public equipment model before the frontend consumes it. This preserves the existing rule that raw Nexon payloads stay private and gives missing optional fields a stable representation. Governs R6, R8, R12.
- KTD6. **Use the latest successful snapshot:** Resolve equipment from the latest successful snapshot already used by the dashboard. Do not trigger a second Nexon request when opening an item detail view. Governs R1, R5, R7, R9.
- KTD7. **One public equipment shape for list and detail:** The list and detail view shall use the same normalized item data, with the list selecting an item by a stable item identity. Governs R2, R3, R4, R5.
- KTD8. **Preserve existing API error semantics:** Equipment unavailable, cached failure, and empty data states shall follow the existing dashboard response and frontend state conventions. Governs R9, R10.

### High-Level Technical Design

The backend reads the active equipment collection from the latest snapshot, normalizes supported item fields, and returns only the fields required by the list and detail views. The frontend renders a dashboard equipment section and navigates to a dedicated item detail view using the normalized item identity. Optional detail groups are omitted when their source values are absent or invalid.

The existing character image proxy pattern should be reused for equipment image URLs when the source host and path satisfy the same safety boundary. The implementation must not expose raw snapshot JSON or make a browser-side Nexon request.

### Implementation Constraints

- Keep active equipment distinct from presets, cash equipment, and job-specific collections.
- Preserve `null`/unknown values as unknown during normalization; never coerce missing numeric attributes to zero.
- Keep the detail view scoped to the latest successful snapshot and show its captured date/time as context.
- Reuse existing dashboard loading, cached failure, retryable error, mobile, and accessibility patterns.
- Keep the current equipment list usable when an individual optional attribute cannot be normalized.

### Sequencing

1. Define and test the normalized backend equipment contract.
2. Add the public equipment retrieval path and backend error/empty behavior.
3. Add frontend types, equipment list entry point, dedicated detail view, and navigation states.
4. Add responsive/accessibility coverage, documentation updates, and smoke verification.

### Risks and Dependencies

- Nexon item payloads can vary by item type, so the normalizer must tolerate absent optional groups and unknown fields.
- Equipment image URLs require the same allow-list and failure behavior as existing character images.
- A large item detail surface can become difficult to scan on mobile; responsive grouping and progressive reading order need explicit browser coverage.

## Implementation Units

### U1. Normalize current equipment data

- **Goal:** Produce a narrow, stable equipment model from the latest snapshot's active equipment collection.
- **Requirements:** R1, R2, R3, R5, R6, R7, R8, R12.
- **Files:** `backend/src/main/java/com/maple/growth/service/`, `backend/src/main/java/com/maple/growth/dto/api/`, `backend/src/main/java/com/maple/growth/controller/`, `backend/src/test/java/com/maple/growth/service/`, `backend/src/test/java/com/maple/growth/controller/`.
- **Approach:** Reuse the existing raw JSON storage and active-equipment filtering rules. Normalize identity, slot, image, and supported optional attribute groups. Return only populated groups and preserve unknown values as absent.
- **Test Scenarios:** Active rows appear in stable slot order; presets and non-active collections are excluded; missing identity fields do not create broken items; optional groups are omitted when absent; numeric unknowns remain unknown; public responses contain no raw payload.
- **Verification:** Backend unit and controller tests cover populated, partial, empty, malformed, and secret-non-exposure responses.

### U2. Add equipment retrieval and state behavior

- **Goal:** Make the current equipment contract available from the character dashboard flow without a second external fetch per item.
- **Requirements:** R1, R4, R7, R9, R10, R12.
- **Files:** `backend/src/main/java/com/maple/growth/controller/CharacterController.java`, `backend/src/main/java/com/maple/growth/service/CharacterLookupService.java`, `backend/src/main/java/com/maple/growth/service/SnapshotSyncService.java`, `backend/src/main/java/com/maple/growth/dto/api/`, `backend/src/test/java/com/maple/growth/controller/CharacterControllerTest.java`, `backend/src/test/java/com/maple/growth/service/SnapshotSyncServiceTest.java`.
- **Approach:** Extend the existing character data flow with a normalized equipment response or dashboard projection. Preserve cached dashboard behavior and distinguish empty equipment from unavailable equipment.
- **Test Scenarios:** Latest successful snapshot supplies equipment; no snapshot returns the documented empty/unavailable state; cached data remains visible after Nexon failure; a missing item identity does not produce a navigable detail target; repeated detail requests do not trigger Nexon calls.
- **Verification:** Backend test suite passes with API wrapper shape and failure mapping assertions.

### U3. Build the equipment list and dedicated detail view

- **Goal:** Let users scan current equipment and open a dedicated detail view for one item.
- **Requirements:** R2, R3, R4, R5, R6, R7, R8, R10, R11.
- **Files:** `frontend/app/character/[name]/`, `frontend/components/CharacterDashboardClient.tsx`, `frontend/components/CharacterDashboardView.tsx`, `frontend/components/`, `frontend/lib/api/client.ts`, `frontend/lib/api/types.ts`, `frontend/__tests__/dashboard-page.test.tsx`, `frontend/__tests__/dashboard-client.test.tsx`, `frontend/__tests__/api-client.test.tsx`.
- **Approach:** Add a compact equipment list to the dashboard and a dedicated item detail route/view. Reuse existing character route encoding, API wrapper handling, loading states, and image proxy conventions. Keep the list summary distinct from the full detail groups.
- **Test Scenarios:** List renders populated slots; item selection reaches the detail view and back navigation preserves context; item identity and available sections render; absent sections are hidden; empty and unavailable states render correctly; encoded character names remain safe; cached dashboard content remains visible during refresh.
- **Verification:** Frontend unit tests cover API mapping, list/detail rendering, navigation state, optional fields, and failure states.

### U4. Verify responsive and accessible inspection

- **Goal:** Prove that current equipment inspection works on desktop, mobile, and keyboard navigation.
- **Requirements:** R9, R10, R11.
- **Files:** `frontend/styles/globals.css`, `frontend/scripts/e2e-regression.mjs`, `frontend/__tests__/dashboard-page.test.tsx`, `doc/api/api_contract.md`, `doc/ui/ui_states.md`, `README.md`.
- **Approach:** Define the reading order for long detail sections, ensure no horizontal overflow, expose text labels and state semantics, and add browser regression checks for desktop/mobile entry, detail navigation, empty data, and retryable failure.
- **Test Scenarios:** Desktop list-to-detail flow; narrow viewport list-to-detail flow; keyboard focus reaches each item and the back control; long optional sections remain readable; image proxy failure does not break the detail page; unavailable equipment displays an actionable state.
- **Verification:** `cd frontend && npm test`, `cd frontend && npm run build`, and `cd frontend && npm run test:e2e` pass.

## Verification Contract

| Check | Command | Applies to | Done signal |
|---|---|---|---|
| Backend behavior | `cd backend && ./gradlew test` | U1, U2 | All backend tests pass, including equipment normalization and API contract cases. |
| Frontend behavior | `cd frontend && npm test` | U3, U4 | All frontend tests pass, including list/detail and state cases. |
| Frontend build | `cd frontend && npm run build` | U3, U4 | Production build completes without type or route errors. |
| Browser regression | `cd frontend && npm run test:e2e` | U4 | Desktop/mobile equipment inspection and error states pass. |
| Compose/config hygiene | `docker compose config` | U1-U4 | Configuration remains valid and no secrets enter frontend-facing variables. |

## Definition of Done

- U1-U4 are implemented with tests covering their listed scenarios.
- The dashboard exposes current active equipment and each item opens a dedicated detail view.
- Detail sections appear only for values that exist in the normalized item data.
- Historical comparison, recommendations, scoring, and deferred equipment collections remain absent from the feature.
- Backend responses do not expose raw Nexon JSON or secrets.
- Backend tests, frontend tests, production build, browser regression, and relevant configuration checks pass.
- No abandoned experiments, generated build outputs, or secret files remain in the final diff.
