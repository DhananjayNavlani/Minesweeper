package com.example.minesweeper

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import com.example.minesweeper.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private lateinit var b:ActivityMainBinding
    var custom = false
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityMainBinding.inflate(layoutInflater)
        val view = b.root
        setContentView(view)

        val sharedPref = getSharedPreferences("Scores", Context.MODE_PRIVATE)
        var bestTime:Int = sharedPref.getInt("BEST_TIME",0)
        var lstGameTime:Int = sharedPref.getInt("LAST_GAME_TIME",0)

        b.bestTime.text = "Best Time: $bestTime s"
        b.lastGameTime.text = "Last Game Time: $lstGameTime s"

        b.btnCustomBoard.setOnClickListener {
            if(!custom){
                b.customRow.visibility = View.VISIBLE
                b.customCols.visibility = View.VISIBLE
                b.customMines.visibility = View.VISIBLE
            }else{
                b.customRow.visibility = View.GONE
                b.customCols.visibility = View.GONE
                b.customMines.visibility = View.GONE
            }
            custom = !custom
        }

        b.btnStart.setOnClickListener {
            if(custom){
                makeCustomBoard()
            }else{
                getSelectedLevel()
            }
        }

    }

    private fun getSelectedLevel() {
     /*
        Get the checked radio button
        By default,
        for easy mode, rows=9, columns=9, mines=20
        for medium mode, rows=16, columns=16, mines=63
        for hard mode, rows=30, columns=16, mines=119
        and invoke startGame function which take rows, columns and mines as parameters

     */
        when(b.rgDifficulty.checkedRadioButtonId){
            b.rbEasy.id -> startGame(9,9,20)
            b.rbMed.id -> startGame(16,16,63)
            b.rbHard.id -> startGame(30,16,119)
        }
    }

    private fun makeCustomBoard() {
        //checking if any edit text is blank
        if(b.customRow.text.isBlank()){
            b.customRow.error = "Field required"
            return
        }
        if(b.customCols.text.isBlank()){
            b.customRow.error = "Field required"
            return
        }
        if(b.customMines.text.isBlank()){
            b.customRow.error = "Field required"
            return
        }

        val row = b.customRow.text.toString().toInt()
        val col = b.customCols.text.toString().toInt()
        val mines = b.customMines.text.toString().toInt()

        //check if the number of mines is less than 1/4th of the board’s button to avoid
        // overcrowding of mines.
        val threshold = (row * col) / 4
        if(mines >= threshold){
            b.customMines.error = "Mines should be less than $threshold"
            return
        }

        startGame(row,col,mines)
    }

    private fun startGame(row: Int, col: Int, mines: Int) {
        val intent = Intent(this,GameActivity::class.java).apply {
            putExtra("ROWS",row)
            putExtra("COLUMNS",col)
            putExtra("MINES",mines)
        }

        startActivity(intent)

    }
}