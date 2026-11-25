package org.fppuck.community.data

/**
 * Pure Kotlin parser for the SHOTS (shots on goal) table embedded in a tab-delimited
 * box score document.
 *
 * The table is expected to look like:
 *
 *   SHOTS
 *   Team	1st	2nd	3rd	T        (optional OT column between 3rd and T)
 *   HomeTeam	10	12	9	31
 *   AwayTeam	8	14	11	33
 *
 * Rules:
 *  - The exact "SHOTS" heading (case-insensitive, trimmed) must be present;
 *    otherwise the table is unavailable.
 *  - Real blank spacer lines are allowed between the heading, the header row and
 *    the data rows; they never terminate the table.
 *  - The header row declares Team, 1st, 2nd, 3rd and T (an OT column between
 *    3rd and T is optional). Missing required cells reject the whole table.
 *  - Column positions are tab positions from the header; cells are never compacted.
 *  - A malformed data row is skipped without suppressing the other team's valid
 *    row. Counts are never guessed, and T is read from the published T column,
 *    never recomputed from period columns.
 *  - Collection is bounded to the two expected team rows (including a malformed
 *    tabular slot), so trailing lookalike rows cannot be consumed.
 *  - Parsing stops at a standalone next-section heading.
 *  - Row order is preserved, real zeros are kept, original team names verbatim.
 *  - Missing/invalid tables return an empty list (statistics unavailable).
 */

private const val SHOTS_HEADING = "SHOTS"

/** The SHOTS table reports exactly two teams; that many tabular slots are consumed. */
private const val MAX_SHOT_ROWS = 2

internal fun parseShotsOnGoal(lines: List<String>): List<GamePeriodBreakdown> {
    val headingIndex = lines.indexOfFirst { it.trim().equals(SHOTS_HEADING, ignoreCase = true) }
    if (headingIndex < 0) return emptyList()

    // Locate the header row: real spacer blank lines between heading and header are allowed.
    var i = headingIndex + 1
    while (i < lines.size && lines[i].isBlank()) i++
    if (i >= lines.size) return emptyList()

    val header = splitCells(lines[i])
    if (header.isEmpty()) return emptyList()

    var teamCol = -1
    var firstCol = -1
    var secondCol = -1
    var thirdCol = -1
    var otCol = -1
    var totalCol = -1

    for ((j, cell) in header.withIndex()) {
        when (cell.lowercase()) {
            "team" -> if (teamCol >= 0) return emptyList() else teamCol = j
            "1st" -> if (firstCol >= 0) return emptyList() else firstCol = j
            "2nd" -> if (secondCol >= 0) return emptyList() else secondCol = j
            "3rd" -> if (thirdCol >= 0) return emptyList() else thirdCol = j
            "ot" -> if (otCol >= 0) return emptyList() else otCol = j
            "t" -> if (totalCol >= 0) return emptyList() else totalCol = j
        }
    }

    // Required columns must all be present; OT is optional. Total comes from T, never OT.
    if (teamCol < 0 || firstCol < 0 || secondCol < 0 || thirdCol < 0 || totalCol < 0) {
        return emptyList()
    }

    val rows = mutableListOf<GamePeriodBreakdown>()
    i++
    var slotsConsumed = 0
    while (i < lines.size && slotsConsumed < MAX_SHOT_ROWS) {
        // Real spacer blank lines between data rows are skipped, not a table terminator.
        if (lines[i].isBlank()) {
            i++
            continue
        }

        val cells = splitCells(lines[i])
        // A standalone next-section heading terminates the table.
        if (isSectionBoundary(cells)) break

        slotsConsumed++
        i++

        // Required cells must exist, be valid non-negative integers; reject coercion.
        val first = parseNonNegativeInt(cellAt(cells, firstCol)) ?: continue
        val second = parseNonNegativeInt(cellAt(cells, secondCol)) ?: continue
        val third = parseNonNegativeInt(cellAt(cells, thirdCol)) ?: continue
        val total = parseNonNegativeInt(cellAt(cells, totalCol)) ?: continue

        // Team name is required and must be non-empty.
        val teamName = cellAt(cells, teamCol)
        if (teamName.isEmpty()) continue

        rows += GamePeriodBreakdown(
            teamName = teamName,
            first = first,
            second = second,
            third = third,
            total = total
        )
    }

    return rows
}

private fun splitCells(line: String): List<String> {
    // Tab-delimited cells; strip trailing whitespace per cell.
    if (line.isBlank()) return emptyList()
    return line.split("\t").map { it.trim() }
}

private fun cellAt(cells: List<String>, index: Int): String =
    if (index in cells.indices) cells[index] else ""

private fun isSectionBoundary(cells: List<String>): Boolean {
    // A row that starts with a non-numeric, non-team-looking label that matches a
    // known next-section marker terminates the table.
    val first = cells[0]
    // Known section labels terminate the table.
    val upper = first.uppercase()
    if (upper == "3 STARS" || upper == "STARS" || upper == "PENALTIES" ||
        upper == "GOALS" || upper == "SCORING" || upper == "OFFICIALS" ||
        upper == "ATTENDANCE" || upper == "GAME"
    ) {
        return true
    }
    // A standalone nonblank heading terminates the table.
    if (cells.size == 1) {
        return true
    }
    // A tabular slot with a blank team cell is malformed: reject it as a value,
    // consume it, and keep scanning so a valid opponent row below is not hidden.
    return false
}

private fun parseNonNegativeInt(value: String): Int? {
    // Reject blank, negative, malformed, decimal, whitespace-embedded values.
    if (value.isEmpty()) return null
    var i = 0
    while (i < value.length) {
        val c = value[i]
        if (c < '0' || c > '9') return null
        i++
    }
    // Guard against overflow / leading-zero-only strings.
    return value.toIntOrNull()?.takeIf { it >= 0 }
}
