package com.example.guessplayer.chapter_tools

class FootballClub(val clubImage: Int?, val transferYear: String) {
    fun isLoanTransfer(): Boolean {
        return transferYear.contains("_(L)")
    }

    fun getCleanTransferYear(): String {
        return transferYear.replace("_(L)", "")
    }
}
