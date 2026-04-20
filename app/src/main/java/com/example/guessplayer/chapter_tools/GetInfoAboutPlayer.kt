package com.example.guessplayer.chapter_tools

import android.content.Context
import android.util.Log
import java.io.FileNotFoundException

class GetInfoAboutPlayer {

    companion object {
        fun getFootballPlayersClubs(
            context: Context,
            filenameForFootballPlayersClubs: String
        ): List<List<String>> {
            return try {
                context.assets.open(filenameForFootballPlayersClubs).bufferedReader()
                    .use { reader ->
                        reader.readLines().map { line ->
                            line.trim().split("\\s+".toRegex())
                        }
                    }
            } catch (e: Exception) {
                Log.e("FileRead", "Error with reading file $filenameForFootballPlayersClubs", e)
                emptyList()
            }
        }

        fun getFootballPlayersTransferYears(
            context: Context,
            filenameForFootballPlayersTransferYears: String
        ): List<List<String>> {
            return try {
                context.assets.open(filenameForFootballPlayersTransferYears).bufferedReader()
                    .use { reader ->
                        reader.readLines().map { line ->
                            line.trim().split("\\s+".toRegex())
                        }
                    }
            } catch (e: Exception) {
                Log.e(
                    "FileRead",
                    "Error with reading file $filenameForFootballPlayersTransferYears",
                    e
                )
                emptyList()
            }
        }

        fun getFootballPlayersNames(
            context: Context,
            filenameForGetFootballPlayersNames: String
        ): List<String> {
            return try {
                context.assets.open(filenameForGetFootballPlayersNames).bufferedReader()
                    .useLines { lines ->
                        lines.map { it.trim() }
                            .filter { it.isNotEmpty() }
                            .toList()
                    }
            } catch (e: Exception) {
                Log.e("FileRead", "Error with reading file: $filenameForGetFootballPlayersNames", e)
                emptyList()
            }
        }

        fun getCurrentLevelFromFile(
            context: Context,
            filenameForGameProgress: String,
            currentChapter: Int
        ): Int {
            return try {
                context.openFileInput(filenameForGameProgress).bufferedReader().useLines { lines ->
                    lines.map { line ->
                        val parts = line.trim().split("\\s+".toRegex())
                        if (parts.size == 2 && parts[0] == "chapter$currentChapter") {
                            parts[1].toIntOrNull() ?: 0
                        } else {
                            0
                        }
                    }.firstOrNull() ?: 0
                }
            } catch (e: FileNotFoundException) {
                Log.e("FileRead", "File $filenameForGameProgress not found, creating default", e)
                // Создаем файл с начальным прогрессом
                updateProgressInFile(context, 1, currentChapter)
                1
            } catch (e: Exception) {
                Log.e("FileRead", "Error with reading file $filenameForGameProgress", e)
                1
            }
        }

        fun getBalanceFromFile(context: Context, filenameForGameProgress: String): Int {
            return try {
                context.openFileInput(filenameForGameProgress).bufferedReader().useLines { lines ->
                    lines.firstNotNullOfOrNull { line ->
                        val parts = line.trim().split("\\s+".toRegex())
                        if (parts.size == 2 && parts[0] == "balance") {
                            parts[1].toIntOrNull()
                        } else {
                            null
                        }
                    } ?: 0
                }
            } catch (e: FileNotFoundException) {
                Log.e("FileRead", "File $filenameForGameProgress not found", e)
                0
            } catch (e: Exception) {
                Log.e("FileRead", "Error with reading balance from $filenameForGameProgress", e)
                0
            }
        }

        fun getUserLevelFromFile(context: Context, filenameForGameProgress: String): Int {
            return try {
                context.openFileInput(filenameForGameProgress).bufferedReader().useLines { lines ->
                    lines.mapNotNull { line ->
                        val parts = line.trim().split("\\s+".toRegex())
                        if (parts.size == 2 && parts[0] == "user_level") {
                            parts[1].toIntOrNull()
                        } else {
                            null
                        }
                    }.firstOrNull() ?: 0
                }
            } catch (e: FileNotFoundException) {
                Log.e("FileRead", "File $filenameForGameProgress not found", e)
                0
            } catch (e: Exception) {
                Log.e("FileRead", "Error with reading balance from $filenameForGameProgress", e)
                0
            }
        }

        fun readProgressFile(context: Context): List<String> {
            return try {
                context.openFileInput("game_progress.txt").bufferedReader().useLines { it.toList() }
            } catch (e: FileNotFoundException) {
                // Исправленная строка:
                val initialContent =
                    listOf("chapter1 1", "chapter2 1", "chapter3 1", "balance 0", "user_level 0")
                // Создаем файл с начальными значениями
                context.openFileOutput("game_progress.txt", Context.MODE_PRIVATE).use { output ->
                    output.write(initialContent.joinToString("\n").toByteArray())
                }
                initialContent
            } catch (e: Exception) {
                Log.e("FileRead", "Error with reading file game_progress.txt", e)
                emptyList()
            }
        }

        fun updateProgressInFile(
            context: Context, newLevel: Int, currentChapter: Int,
            newBalance: Int? = null, newUserLevel: Int? = null
        ) {
            val fileName = "game_progress.txt"

            try {
                val currentContent = readProgressFile(context)
                val updatedContent = mutableListOf<String>()

                var chapterFound = false
                var balanceFound = false
                var userLevelFound = false

                for (line in currentContent) {
                    when {
                        line.startsWith("chapter$currentChapter") -> {
                            updatedContent.add("chapter$currentChapter $newLevel")
                            chapterFound = true
                        }

                        newBalance != null && line.startsWith("balance") -> {
                            updatedContent.add("balance $newBalance")
                            balanceFound = true
                        }

                        newUserLevel != null && line.startsWith("user_level") -> {
                            updatedContent.add("user_level $newUserLevel")
                            userLevelFound = true
                        }

                        else -> {
                            updatedContent.add(line)
                        }
                    }
                }

                // Добавляем главу, если не найдена
                if (!chapterFound) {
                    updatedContent.add("chapter$currentChapter $newLevel")
                }

                // Добавляем баланс, если передан newBalance и баланс не был найден
                if (newBalance != null && !balanceFound) {
                    updatedContent.add("balance $newBalance")
                }

                if (newUserLevel != null && !userLevelFound) {
                    updatedContent.add("user_level $newUserLevel")
                }

                // Записываем обновленное содержимое
                context.openFileOutput(fileName, Context.MODE_PRIVATE).use { output ->
                    output.write(updatedContent.joinToString("\n").toByteArray())
                }

                Log.d("ProgressUpdate", "Progress updated.")

            } catch (e: Exception) {
                Log.e("ProgressUpdate", "Error with updating: chapter$currentChapter $newLevel", e)
            }
        }
    }
}
