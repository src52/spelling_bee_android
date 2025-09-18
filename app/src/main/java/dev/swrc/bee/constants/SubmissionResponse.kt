package dev.swrc.bee.constants

enum class SubmissionResponse(val response: Response, val valid: Boolean) {
    ALREADY_FOUND(Response.ALREADY_FOUND, false), INVALID_WORD(Response.INVALID_WORD, false)
    , VALID_WORD(Response.VALID_WORD, true), TOO_SHORT(Response.TOO_SHORT, false), MISSING_CENTER_LETTER(
        Response.MISSING_CENTER_LETTER, false);
}