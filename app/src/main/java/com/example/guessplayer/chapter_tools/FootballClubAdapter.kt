package com.example.guessplayer.chapter_tools

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.guessplayer.R

class FootballClubAdapter(
    private val allFootballClubList: List<FootballClub>
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val TYPE_NORMAL = 0
        private const val TYPE_LOAN = 1
    }

    private var showLoanTransfers = true
    private var filteredList: List<FootballClub> = allFootballClubList

    override fun getItemViewType(position: Int): Int {
        return if (filteredList[position].isLoanTransfer()) {
            TYPE_LOAN
        } else {
            TYPE_NORMAL
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            TYPE_LOAN -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.fc_recyclerview_2, parent, false)
                LoanViewHolder(view)
            }
            else -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.fc_recyclerview_1, parent, false)
                NormalViewHolder(view)
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val footballClub = filteredList[position]

        when (holder) {
            is NormalViewHolder -> {
                holder.footballClubImage.setImageResource(footballClub.clubImage!!)
                holder.transferYearTV.text = footballClub.getCleanTransferYear()
            }
            is LoanViewHolder -> {
                holder.footballClubImage.setImageResource(footballClub.clubImage!!)
                holder.transferYearTV.text = footballClub.getCleanTransferYear()
            }
        }
    }

    override fun getItemCount(): Int = filteredList.size

    @SuppressLint("NotifyDataSetChanged")
    fun filterLoans(showLoans: Boolean) {
        showLoanTransfers = showLoans

        filteredList = if (showLoans) {
            allFootballClubList
        } else {
            allFootballClubList.filter { !it.isLoanTransfer() }
        }

        notifyDataSetChanged()
    }

    fun getHiddenCount(): Int {
        return if (showLoanTransfers) {
            0
        } else {
            allFootballClubList.count { it.isLoanTransfer() }
        }
    }

    class NormalViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val footballClubImage: ImageView = itemView.findViewById(R.id.imageview)
        val transferYearTV: TextView = itemView.findViewById(R.id.textView)
    }

    class LoanViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val footballClubImage: ImageView = itemView.findViewById(R.id.imageview)
        val transferYearTV: TextView = itemView.findViewById(R.id.textView)
    }
}