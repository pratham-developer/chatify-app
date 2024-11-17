package com.pratham.chatify

import android.content.res.Resources
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.pratham.chatify.R
import com.pratham.chatify.Message

class MessageAdapter(
    private val messages: List<Message>,
    private val currentUserId: String
) : RecyclerView.Adapter<MessageAdapter.MessageViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_message, parent, false)
        return MessageViewHolder(view)
    }

    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        val message = messages[position]
        holder.bind(message, currentUserId)
    }

    override fun getItemCount(): Int = messages.size

    class MessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val screenWidth = Resources.getSystem().displayMetrics.widthPixels
        private val maxCardWidth = (screenWidth * 0.6).toInt() // 60% of screen width
        private val messageCard: CardView = itemView.findViewById(R.id.message_card)
        private val messageText: TextView = itemView.findViewById(R.id.message_text)
        private val messageSender: TextView = itemView.findViewById(R.id.message_sender)

        fun bind(message: Message, currentUserId: String) {
            messageText.text = message.text

            // Measure the text width
            val textWidth = messageText.paint.measureText(message.text).toInt()
            val layoutParams = messageCard.layoutParams

            // Set width dynamically
            layoutParams.width = if (textWidth > maxCardWidth) maxCardWidth else ViewGroup.LayoutParams.WRAP_CONTENT
            messageCard.layoutParams = layoutParams

            // Adjust alignment and background color
            val params = messageCard.layoutParams as ConstraintLayout.LayoutParams
            if (message.senderId == currentUserId) {
                // Align to the right for current user
                params.startToStart = ConstraintLayout.LayoutParams.UNSET
                params.endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
                messageCard.setCardBackgroundColor(
                    ContextCompat.getColor(itemView.context, R.color.tertiary)
                )
                messageText.setTextColor(ContextCompat.getColor(itemView.context, R.color.secondary))
                messageSender.visibility = View.GONE
            } else {
                // Align to the left for other users
                params.endToEnd = ConstraintLayout.LayoutParams.UNSET
                params.startToStart = ConstraintLayout.LayoutParams.PARENT_ID
                messageCard.setCardBackgroundColor(
                    ContextCompat.getColor(itemView.context, R.color.primary)
                )
                messageSender.setTextColor(ContextCompat.getColor(itemView.context, R.color.green))
                messageText.setTextColor(ContextCompat.getColor(itemView.context, R.color.tertiary))
                messageSender.visibility = View.VISIBLE
                messageSender.text = message.senderName
            }
            messageCard.layoutParams = params
        }
    }
}
