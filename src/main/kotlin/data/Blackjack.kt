package gay.spiders.data

import dev.kord.common.entity.ButtonStyle
import dev.kord.rest.builder.message.MessageBuilder
import dev.kord.rest.builder.message.actionRow
import gay.spiders.popLast

enum class Suit {
    CLUBS, DIAMONDS, HEARTS, SPADES;

    val displayName = this.name.lowercase().replaceFirstChar { it.uppercase() }
}

enum class Rank(val value: Int) {
    TWO(2), THREE(3), FOUR(4), FIVE(5), SIX(6), SEVEN(7), EIGHT(8), NINE(9), TEN(10), JACK(10), QUEEN(10), KING(10), ACE(
        11
    );

    val displayName = this.name.lowercase().replaceFirstChar { it.uppercase() }
}

data class Card(val rank: Rank, val suit: Suit) {
    val displayName = "${rank.displayName} of ${suit.displayName}"
}

data class Deck(var cards: MutableList<Card> = mutableListOf()) {
    companion object {
        fun newShuffledDeck(): Deck {
            val cards = Suit.entries.flatMap { suit ->
                Rank.entries.map { rank ->
                    Card(rank, suit)
                }
            }.shuffled()
            return Deck(cards.toMutableList())
        }
    }

    fun calculate(threshold: Int = 21): Int {
        var sum = cards.sumOf { it.rank.value }
        var aces = cards.count { it.rank == Rank.ACE }
        while (aces > 0) {
            if (sum <= threshold) return sum else {
                sum -= 10
                --aces
            }
        }
        return sum
    }

    fun isBlackjack(): Boolean = cards.size == 2 && calculate() == 21
}

data class Game(val deck: Deck, val hand: Deck, val dealer: Deck) {
    companion object {
        fun newGame(): Game {
            val deck = Deck.newShuffledDeck()
            return Game(deck, Deck(), Deck())
        }
    }

    fun reset() {
        deck.cards = Deck.newShuffledDeck().cards
        hand.cards.clear()
        dealer.cards.clear()
    }

    fun deal() {
        hand.cards.addAll(deck.cards.popLast(2))
        dealer.cards.addAll(deck.cards.popLast(1))
    }

    fun hit(): Boolean {
        hand.cards.addAll(deck.cards.popLast(1))
        return hand.calculate() <= 21
    }

    fun stand(): Boolean {
        while (dealer.calculate(17) < 17) dealer.cards.addAll(deck.cards.popLast(1))
        return dealer.calculate() < hand.calculate() || (dealer.isBlackjack().not() && hand.isBlackjack())
    }
}

fun blackjackString(game: Game, string: String? = null): String {
    val output = string?.let { it + "\n\n" }.orEmpty()
    return output + """Dealer's hand: ${game.dealer.cards.joinToString { it.displayName }}
                    |Dealer's total: ${game.dealer.calculate()}
                    |Your hand: ${game.hand.cards.joinToString { it.displayName }}
                    |Your total: ${game.hand.calculate()}
                    """.trimMargin()
}

fun MessageBuilder.blackjackButtons() {
    actionRow {
        interactionButton(
            style = ButtonStyle.Primary, customId = "hit"
        ) { label = "Hit" }
        interactionButton(
            style = ButtonStyle.Primary, customId = "stand"
        ) { label = "Stand" }
    }
}
