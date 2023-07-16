package com.example.minesweeper

import android.content.Context
import android.content.Intent
import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import com.example.minesweeper.databinding.ActivityGameBinding
import java.util.Locale

class GameActivity : AppCompatActivity() {
    //constant symbol for a mine
    private val MINE = -1

    //default count of rows=9, columns=9, mines=20
    private var rows: Int = 9
    private var columns: Int = 9
    private var mines: Int = 20

    private var count: Int = 0 //integer variable to check first move
    private var flagCount: Int = 0 //it contains count of buttons marked as flag
    private var time = "0" //to store time elapsed

    private lateinit var board: Array<Array<MineButton>>
    private var flag = false// true if flag button is selected
    private val movement = intArrayOf(-1, 0, 1)

    //the following variables are used for timer working
    private var seconds = 0 //this variable increases by 1 per second
    private var running = false //stores the state of the game
    private var wasRunning = false //stores the state of the timer before the activity goes in paused state
    private val handler = Handler()
    private lateinit var runnable: Runnable

    private lateinit var b:ActivityGameBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b  = ActivityGameBinding.inflate(layoutInflater)
        val view = b.root
        setContentView(view)

        startTimer()//initializing timer

        rows = intent.getIntExtra("ROWS",9)
        columns = intent.getIntExtra("COLUMNS",9)
        mines = intent.getIntExtra("MINES",20)

        board = Array(rows){ Array(columns) { MineButton() } }

        setupBoard()//setting up board according to user selection

        b.btnRestart.setOnClickListener {
            restartGame() //restart the game
        }

        b.ibMineFlag.setOnClickListener {
            //this button toggles the flag-mine selection and selects the background accordingly
            if(count != 0){
                if (flag)
                    b.ibMineFlag.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.flag))
                else
                    b.ibMineFlag.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.mine))

                flag = !flag
            }else{
                Toast.makeText(this,"The first move can't be flag",Toast.LENGTH_LONG).show()
            }

        }

    }

    private fun setupBoard() {
        //parameters for linear layout
        val param1 = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            0,1f
        )

        //parameters for button i.e board boxes
        val param2 = LinearLayout.LayoutParams(
            0,
            150,1f
        )

        for(i in 0 until rows){
            val linearLayout = LinearLayout(this)
            linearLayout.orientation = LinearLayout.HORIZONTAL
            linearLayout.layoutParams = param1
//            param1.weight = 1.0F

            for(j in 0 until columns){
                val bt = Button(this)
                bt.layoutParams = param2
//                param2.weight = 1.0F
                bt.setBackgroundResource(R.drawable.button)

                bt.setOnClickListener {
                    recordMove(i,j)
                }
                linearLayout.addView(bt)
            }

            b.llBoard.addView(linearLayout)
        }

        flagCount = mines
        b.tvMines.text = "$flagCount"
        b.tvTime.text = "$time"
    }

    private fun recordMove(x: Int, y: Int) {
        //getting selected button
        val bt = getButton(x,y)
        /*

        If the count==0 ie user played his first move, then 3 things are done :-
        1. Mines are set
        2. Immediate non-mine neighbours to the selected button are revealed
        3. Timer is started

        */

        if(count==0){
            count++

            //start the timer
            running = true

            //set the mines
            setMines(x,y)

            //reveal immediate non-mine neighbours
            for(i in movement){
                for(j in movement){
                    if(!(i==0 && j==0) && ((x+i in 0 until rows) && (y+j) in 0 until columns))
                        reveal(x+i,y+j) //reveal the neighbours
                }
            }
        }


        if (flag) {
            /*
            This toggles the button state as flagged or not
             */
            if (board[x][y].isMarked) {
                /*
                If the button is already marked as flag, then un-mark it and update its attributes
                to default state
                 */
                board[x][y].isMarked = !board[x][y].isMarked
                bt.text = "" //set the button as blank as it is not flagged and not revealed now
                bt.setBackgroundResource(R.drawable.button)
                //update the flag count
                flagCount++
                b.tvMines.text = "$flagCount"
            } else {
                /*
                If the flagCount=0, it means user has marked all the mines as flag. Hence, no
                further flags are allowed.
                 */
                if (flagCount > 0) {
                    //Mark the button as flag
                    board[x][y].isMarked = !board[x][y].isMarked
                    //update the button UI
                    bt.setBackgroundResource(R.drawable.flag_button)
                    bt.background = ContextCompat.getDrawable(this, R.drawable.flag)
                    //Decrease the flag count
                    flagCount--
                    b.tvMines.text = "$flagCount"
                }
            }
        } else {
            if (board[x][y].isMarked || board[x][y].isRevealed) {
                //If the button is marked as flag or has been already revealed, then do nothing
                return
            }
            if (board[x][y].value == MINE) {
                //If the user clicks a mine, then he losses the game
                gameLost()
            } else {
                //Reveal the button
                reveal(x, y)
            }
        }

        //check whether user has completed the game
        if (isComplete()) {
            //If true, it means user has won the game
            running = false //stop the timer
            disableAllButtons() //disable the board for further moves
            //update the user that he has won
            Toast.makeText(this, "Congratulations! You won.", Toast.LENGTH_LONG).show()
            updateScores("won") //update the scores
            showDialog() //show a share dialog
        }

    }

    private fun reveal(x: Int, y: Int) {
        if(!board[x][y].isRevealed && !board[x][y].isMarked && board[x][y].value != MINE){

            //get the button to be revealed
            val button = getButton(x, y)
            button.text = board[x][y].value.toString() //sets the mine count as text
            button.isEnabled = false //disable the button for future clicks
            board[x][y].isRevealed = true //set the button isRevealed property to true
            //update the disabled button's UI
            button.setBackgroundResource(R.drawable.disabled_button)
            button.setTextColor(Color.BLACK)

            /*
            If the value of button is 0 ie there is no mine in its neighbourhood,
            reveal all its neighbours
             */
            if(board[x][y].value == 0){
                for(i in movement){
                    for(j in movement){
                        if(!(i==0 && j==0) && ((x+i in 0 until rows) && (y+j) in 0 until columns))
                            reveal(x+i,y+j) //reveal the neighbours
                    }
                }
            }
        }

    }

    private fun setMines(row: Int, column: Int) {
        /*
        row and column represent the position of the first button clicked. So that a mine is not
        set on it
         */

        var i = 1
        while(i<=mines){

            //getting random x,y for setting mines
            var x = (0 until  rows).random()
            var y = (0 until  columns).random()

            //checks the validity
            if(x!=row && y!=column && board[x][y].value != MINE){
                board[x][y].value = MINE

                updateNeighbours(x,y)
                i++
            }
        }
    }

    private fun updateNeighbours(row: Int, col: Int) {
        //updating the neighbours of mine

        for(i in movement){
            for(j in movement){
                if(((row+i) in 0 until rows) && (col+j) in 0 until columns && board[row+i][col+j].value != MINE){
                    //if the neighbour is in the board and is not a mine, its value is incremented
                    //by 1 as a mine is placed in its neighbourhood
                    board[row + i][col + j].value++
                }

            }
        }
    }

    private fun getButton(x: Int, y: Int): Button {
        val rowLayout = b.llBoard.getChildAt(x) as LinearLayout
        return rowLayout.getChildAt(y) as Button
    }

    private fun startTimer() {
        runnable = Runnable { doWork() }

        handler.post(runnable)
    }

    private fun doWork() {
        val hours:Int = seconds / 3600
        val mins:Int = seconds % 3600 / 60
        val secs: Int = seconds % 60

        time = String.format(Locale.getDefault(),"%d:%02d:%02d",hours,mins,secs)

        b.tvTime.text = time
        if(running) seconds++

        handler.postDelayed(runnable,1000)
    }

    private fun gameLost() {
        //Reveal all the mines to make user aware of his wrong move
        revealAllMines()
        //Disable all buttons on board so that no further move is possible
        disableAllButtons()
        //stop the timer
        running = false
        //Update the user that he has lost the round
        Toast.makeText(this, "You Loose. Keep trying!", Toast.LENGTH_LONG).show()
        //update the scores
        updateScores("lost")
    }

    private fun revealAllMines() {
        /*
        Iterate through all the buttons and reveal them
         */
        for (i in 0 until rows) {
            for (j in 0 until columns) {
                if (board[i][j].value == MINE) {
                    val button = getButton(i, j)
                    button.setBackgroundResource(R.drawable.mine)
                }
            }
        }
    }

    private fun disableAllButtons() {
        /*
        Iterate through all the buttons on the board, disable them and update its UI
         */
        for (x in 0 until rows) {
            for (y in 0 until columns) {
                val button = getButton(x, y) //get the required button
                button.isEnabled = false
                button.setTextColor(Color.BLACK)
            }
        }
    }

    private fun updateScores(status: String) {
        //Get the previous best time
        val sharedPreferences = getSharedPreferences("Scores", Context.MODE_PRIVATE)
        var bestTime = sharedPreferences.getInt("BEST_TIME", 0)

        if (status == "won") {
            //best time is only updated if user wins and his score is better than previous score
            if (bestTime == 0) {
                //if best time=0, it means user has won for the first time
                bestTime = seconds
            } else {
                if (seconds < bestTime)
                    bestTime = seconds
            }
        }

        //save the updated scores
        val sharedPreferencesUpdate = getSharedPreferences("Scores", Context.MODE_PRIVATE)
        with(sharedPreferencesUpdate.edit()) {
            putInt("BEST_TIME", bestTime)
            putInt("LAST_GAME_TIME", seconds)
            commit()
        }
    }

    private fun isComplete(): Boolean {
        /*
        This function returns true if all mines have been marked or all the non-mine buttons have
        been revealed
         */

        //this variable is true if all the mines have been marked as flag
        var minesMarked = true
        //if a mine has not been marked as flag, then minesMarked=false
        board.forEach { row ->
            row.forEach {
                if (it.value == MINE) {
                    if (!it.isMarked)
                        minesMarked = false
                }
            }
        }

        //this variable is true if all the non-mine buttons have been revealed
        var valuesRevealed = true
        //Iterate through all the buttons and check if all non-mine buttons have been revealed
        board.forEach { row ->
            row.forEach {
                if (it.value != MINE) {
                    if (!it.isRevealed)
                        valuesRevealed = false
                }
            }
        }

        return minesMarked || valuesRevealed
    }

    private fun showDialog() {
        /*
        This function shows a dialog to the user.
        The dialog asks user whether he wants to share his score with his contacts or not
         */
        val builder = AlertDialog.Builder(this)
        with(builder) {
            setTitle("Share Your Win")
            setMessage("Let others know your great win. Do you wish to share your scores with your friends?")

            setPositiveButton("Yes") { _, _ ->
                //if user wants to share his score, launch the required intent
                val intent = Intent(Intent.ACTION_SEND);
                val body =
                    "Hello, I won the great Minesweeper game. I finished in $seconds seconds."
                intent.type = "text/plain"
                intent.putExtra(Intent.EXTRA_SUBJECT, "I won !")
                intent.putExtra(Intent.EXTRA_TEXT, body)
                startActivity(Intent.createChooser(intent, "Share your win on..."))
            }
            setNegativeButton(
                "No"
            ) { dialog, which -> }

            val alertDialog = builder.create()
            alertDialog.show()
        }
    }

    private fun restartGame() {
        count = 0 //to record user first move
        running = false //to stop the timer
        seconds = 0 //reset the time to 0
        flagCount = mines //reset the number of remaining mines
        flag = false //by default mine button is selected

        /*
        Iterate through all the buttons and do the following :-
        1. Set its neighbouring mine count to 0
        2. Set the isMarked as false as no flag is marked
        3. Set the isRevealed as false as no button is revealed
        4. Set the button text to blank
        5. Enable the button for future clicks
        6. Update its UI
         */
        for (x in 0 until rows) {
            for (y in 0 until columns) {
                board[x][y].value = 0
                board[x][y].isMarked = false
                board[x][y].isRevealed = false

                val button = getButton(x, y)
                button.text = ""
                button.isEnabled = true
                button.setBackgroundResource(R.drawable.button)
            }
        }

        b.tvMines.text = "$flagCount"
        //Reset the mine button
        b.ibMineFlag.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.flag))
    }

    override fun onPause() {
        super.onPause()
        /*
        If the activity pauses, then set the wasRunning parameter as the current running variable
        and pause the timer
         */
        wasRunning = running
        running = false
    }

    override fun onResume() {
        super.onResume()
        /*
        When activity resumes, the if the timer was running before pause, then resume the timer
        also ie make running true
         */
        if (wasRunning)
            running = true
    }
}
