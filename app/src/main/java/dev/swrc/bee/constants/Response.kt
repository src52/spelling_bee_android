package dev.swrc.bee.constants

enum class Response(val message: String) {
    TOO_LONG("Too long!"), TOO_SHORT("Too short!"), ALREADY_FOUND("Already found!"), INVALID_WORD("Invalid word!"),
    VALID_WORD("Good job!"), MISSING_CENTER_LETTER("Missing center letter!");
}