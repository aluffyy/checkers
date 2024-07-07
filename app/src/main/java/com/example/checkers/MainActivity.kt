package com.example.checkers

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import androidx.lifecycle.compose.collectAsStateWithLifecycle

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CheckersTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    CheckersGame()
                }
            }
        }
    }
}

@Composable
fun CheckersTheme(content: @Composable () -> Unit) {
    val customColorScheme = lightColorScheme(
        primary = Color(0xFF6200EE),
        secondary = Color(0xFF03DAC5),
        background = Color(0xFFFAFAFA),
        surface = Color(0xFFFFFFFF),
        onPrimary = Color.White,
        onSecondary = Color.Black,
        onBackground = Color(0xFF121212),
        onSurface = Color(0xFF121212)
    )

    MaterialTheme(
        colorScheme = customColorScheme,
        typography = Typography(),
        content = content
    )
}

@Composable
fun CheckersGame(viewModel: CheckersViewModel = viewModel()) {
    val gameState by viewModel.gameState.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Current Player: ${if (gameState.currentPlayer == Piece.RED) "Red" else "Black"}",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(24.dp))
        CheckersBoard(
            gameState = gameState,
            onCellClick = { row, col -> viewModel.onCellClick(row, col) }
        )
    }
}

@Composable
fun CheckersBoard(gameState: GameState, onCellClick: (Int, Int) -> Unit) {
    val cellSize = 40.dp

    Card(
        modifier = Modifier
            .size(cellSize * 8)
            .padding(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column {
            for (row in 0..7) {
                Row {
                    for (col in 0..7) {
                        CheckerCell(
                            cellSize = cellSize,
                            piece = gameState.board[row][col],
                            isSelected = gameState.selectedCell == Pair(row, col),
                            isLightSquare = (row + col) % 2 == 0,
                            onClick = { onCellClick(row, col) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CheckerCell(
    cellSize: androidx.compose.ui.unit.Dp,
    piece: Piece,
    isSelected: Boolean,
    isLightSquare: Boolean,
    onClick: () -> Unit
) {
    val cellColor = when {
        isSelected -> MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
        isLightSquare -> Color(0xFFF0F0F0)
        else -> Color(0xFFE0E0E0)
    }

    Box(
        modifier = Modifier
            .size(cellSize)
            .background(cellColor)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        when (piece) {
            Piece.RED, Piece.BLACK -> {
                Canvas(modifier = Modifier.size(cellSize * 0.8f)) {
                    drawCircle(
                        color = if (piece == Piece.RED) Color(0xFFE57373) else Color(0xFF616161),
                        radius = size.minDimension / 2
                    )
                }
            }
            Piece.RED_KING, Piece.BLACK_KING -> {
                Canvas(modifier = Modifier.size(cellSize * 0.8f)) {
                    drawCircle(
                        color = if (piece == Piece.RED_KING) Color(0xFFE57373) else Color(0xFF616161),
                        radius = size.minDimension / 2
                    )
                    drawCircle(
                        color = Color(0xFFFFD54F),
                        radius = size.minDimension / 4,
                        style = Stroke(width = 4f)
                    )
                }
            }
            else -> {}
        }
    }
}

enum class Piece { EMPTY, RED, BLACK, RED_KING, BLACK_KING }

data class GameState(
    val board: Array<Array<Piece>> = Array(8) { Array(8) { Piece.EMPTY } },
    val currentPlayer: Piece = Piece.RED,
    val selectedCell: Pair<Int, Int>? = null,
    val redScore: Int = 0,
    val blackScore: Int = 0
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as GameState

        if (!board.contentDeepEquals(other.board)) return false
        if (currentPlayer != other.currentPlayer) return false
        if (selectedCell != other.selectedCell) return false
        if (redScore != other.redScore) return false
        if (blackScore != other.blackScore) return false

        return true
    }

    override fun hashCode(): Int {
        var result = board.contentDeepHashCode()
        result = 31 * result + currentPlayer.hashCode()
        result = 31 * result + (selectedCell?.hashCode() ?: 0)
        result = 31 * result + redScore
        result = 31 * result + blackScore
        return result
    }
}

class CheckersViewModel : ViewModel() {
    private val _gameState = MutableStateFlow(GameState())
    val gameState: StateFlow<GameState> = _gameState

    init {
        initializeBoard()
    }

    private fun initializeBoard() {
        val newBoard = Array(8) { Array(8) { Piece.EMPTY } }
        for (row in 0..2) {
            for (col in 0..7) {
                if ((row + col) % 2 != 0) newBoard[row][col] = Piece.BLACK
            }
        }
        for (row in 5..7) {
            for (col in 0..7) {
                if ((row + col) % 2 != 0) newBoard[row][col] = Piece.RED
            }
        }
        _gameState.value = _gameState.value.copy(board = newBoard)
    }

    fun onCellClick(row: Int, col: Int) {
        val currentState = _gameState.value
        val selectedCell = currentState.selectedCell

        if (selectedCell == null) {
            // If no cell is selected, select this cell if it contains a piece of the current player
            if (currentState.board[row][col] != Piece.EMPTY && currentState.board[row][col].toString().startsWith(currentState.currentPlayer.toString())) {
                _gameState.value = currentState.copy(selectedCell = Pair(row, col))
            }
        } else {
            // If a cell is already selected, try to move the piece
            if (isValidMove(selectedCell.first, selectedCell.second, row, col)) {
                movePiece(selectedCell.first, selectedCell.second, row, col)
            }
            // Deselect the cell
            _gameState.value = _gameState.value.copy(selectedCell = null)
        }
    }

    private fun isValidMove(fromRow: Int, fromCol: Int, toRow: Int, toCol: Int): Boolean {
        // Implement move validation logic here
        // This is a simplified version and doesn't include all checkers rules
        val piece = _gameState.value.board[fromRow][fromCol]
        val rowDiff = toRow - fromRow
        val colDiff = toCol - fromCol

        return when {
            piece == Piece.RED && rowDiff == -1 && kotlin.math.abs(colDiff) == 1 -> true
            piece == Piece.BLACK && rowDiff == 1 && kotlin.math.abs(colDiff) == 1 -> true
            piece == Piece.RED_KING && kotlin.math.abs(rowDiff) == 1 && kotlin.math.abs(colDiff) == 1 -> true
            piece == Piece.BLACK_KING && kotlin.math.abs(rowDiff) == 1 && kotlin.math.abs(colDiff) == 1 -> true
            kotlin.math.abs(rowDiff) == 2 && kotlin.math.abs(colDiff) == 2 -> {
                val capturedRow = (fromRow + toRow) / 2
                val capturedCol = (fromCol + toCol) / 2
                val capturedPiece = _gameState.value.board[capturedRow][capturedCol]
                capturedPiece != Piece.EMPTY && capturedPiece.toString().startsWith(_gameState.value.currentPlayer.toString().reversed())
            }
            else -> false
        }
    }

    private fun movePiece(fromRow: Int, fromCol: Int, toRow: Int, toCol: Int) {
        val currentState = _gameState.value
        val newBoard = currentState.board.map { it.clone() }.toTypedArray()

        val piece = newBoard[fromRow][fromCol]
        newBoard[fromRow][fromCol] = Piece.EMPTY
        newBoard[toRow][toCol] = piece

        var redScore = currentState.redScore
        var blackScore = currentState.blackScore
        var switchPlayer = true

        // Handle captures
        if (kotlin.math.abs(fromRow - toRow) == 2) {
            val capturedRow = (fromRow + toRow) / 2
            val capturedCol = (fromCol + toCol) / 2
            newBoard[capturedRow][capturedCol] = Piece.EMPTY

            // Update score
            if (currentState.currentPlayer == Piece.RED) {
                redScore++
            } else {
                blackScore++
            }
            switchPlayer = false // Allow multiple captures
        }

        // Check for king promotion
        if (piece == Piece.RED && toRow == 0) {
            newBoard[toRow][toCol] = Piece.RED_KING
        } else if (piece == Piece.BLACK && toRow == 7) {
            newBoard[toRow][toCol] = Piece.BLACK_KING
        }

        // Switch player if necessary
        val nextPlayer = if (switchPlayer) {
            if (currentState.currentPlayer == Piece.RED) Piece.BLACK else Piece.RED
        } else {
            currentState.currentPlayer
        }

        _gameState.value = currentState.copy(
            board = newBoard,
            currentPlayer = nextPlayer,
            redScore = redScore,
            blackScore = blackScore
        )
    }
}