package me.pseudoapp

data class Instruction(
    var text: String,
    val type: Type,
    val createdMs: Long = System.currentTimeMillis()
) {
    enum class Type {
        NameValue, //  anything = a
        ResetValue, // {1+1} => a
        StartCondition, // a < b ?
        EndCondition // .
    }


}