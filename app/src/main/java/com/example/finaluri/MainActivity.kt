package com.example.finaluri

import android.annotation.SuppressLint
import android.content.Context
import android.nfc.Tag
import android.os.Bundle
import android.os.PersistableBundle
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.RadioGroup
import android.widget.TextView
import androidx.appcompat.widget.Toolbar
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.vectordrawable.graphics.drawable.ArgbEvaluator
import com.example.finaluri.models.BoardSize
import com.example.finaluri.models.MemoryCard
import com.example.finaluri.models.MemoryGame
import com.example.finaluri.utils.DEFAULT_ICONS
import com.google.android.material.datepicker.MaterialPickerOnPositiveButtonClickListener
import com.google.android.material.snackbar.Snackbar


class MainActivity : AppCompatActivity() {

    companion object {
        private const val  TAG= "MAinActivity"
    }

    private lateinit var toolbar: Toolbar
    private lateinit var clRoot: ConstraintLayout
    private lateinit var rvBoard: RecyclerView
    private lateinit var tvNumMoves: TextView
    private lateinit var tvNumPairs: TextView

    private lateinit var memoryGame: MemoryGame
    private lateinit var adapter: MemoryBoardAdapter
    private var boardSize: BoardSize=BoardSize.EASY


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        clRoot=findViewById(R.id.clRoot)
        rvBoard=findViewById(R.id.rvBoard)
        tvNumMoves=findViewById(R.id.tvNumMoves)
        tvNumPairs=findViewById(R.id.tvNumPairs)


        setupBoard()




        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.clRoot)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main,menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.mi_refresh -> {
                if (memoryGame.getNumMoves() >0 && !memoryGame.haveWonGame()){
                    showAlertDialog("გსურთ თავიდან დაწყება?",null, View.OnClickListener {
                        setupBoard()
                    })
                }else{
                setupBoard()
                }
                return true
            }
            R.id.mi_new_size -> {
                showNewSizeDialog()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun showNewSizeDialog() {
        val boardSizeView = LayoutInflater.from(this).inflate(R.layout.dialog_board_size, null)
        val radioGroupSize = boardSizeView.findViewById<RadioGroup>(R.id.radioGroup)
        when (boardSize) {
            BoardSize.EASY -> radioGroupSize.check(R.id.rbEasy)
            BoardSize.MEDIUM -> radioGroupSize.check(R.id.rbMedium)
            BoardSize.HARD -> radioGroupSize.check(R.id.rbHard)
        }
        showAlertDialog("აირჩიეთ რაოდენობა",boardSizeView, View.OnClickListener {
            boardSize=when (radioGroupSize.checkedRadioButtonId){
                R.id.rbEasy -> BoardSize.EASY
                R.id.rbMedium -> BoardSize.MEDIUM
                else -> BoardSize.HARD
            }
            setupBoard()

        })
    }

    private fun showAlertDialog(title: String, view: View?,positiveClickListener: View.OnClickListener) {
        AlertDialog.Builder(this)
            .setTitle(title)
            .setView(view)
            .setNegativeButton("არა", null)
            .setPositiveButton("დიახ") {_,_->
                positiveClickListener.onClick(null)
            }.show()
    }

    private fun setupBoard() {
        when (boardSize) {
            BoardSize.EASY -> {
                tvNumMoves.text="ადვილი:4 x 2"
                tvNumPairs.text="წყვილი: 0 / 4"
            }
            BoardSize.MEDIUM -> {
                tvNumMoves.text="საშუალო: 6 x 3"
                tvNumPairs.text="წყვილი: 0 / 9"
            }
            BoardSize.HARD -> {
                tvNumMoves.text="რთული:6 x 4"
                tvNumPairs.text="წყვილი: 0 / 12"
            }
        }
        tvNumPairs.setTextColor(ContextCompat.getColor(this,R.color.color_progress_none))
        memoryGame= MemoryGame(boardSize)



        adapter=MemoryBoardAdapter(this,boardSize,memoryGame.cards,object :
            MemoryBoardAdapter.CardClickListener{
            override fun onCardClicked(position: Int) {
                updateGameWithFlip(position)
            }

        })
        rvBoard.adapter= adapter
        rvBoard.setHasFixedSize(true)
        rvBoard.layoutManager= GridLayoutManager(this,boardSize.getWidth())
    }

    @SuppressLint("RestrictedApi")
    private  fun updateGameWithFlip(position: Int) {
        if (memoryGame.haveWonGame()){
            Snackbar.make(clRoot,"შენ უკვე მოიგე!", Snackbar.LENGTH_LONG).show()
            return
        }

        if (memoryGame.isCardFaceUp(position)) {
            Snackbar.make(clRoot,"არასწორი მოძრაობა!", Snackbar.LENGTH_SHORT).show()
            return
        }

        if(memoryGame.flipCard(position)){
            Log.i(TAG, "წყვილი!წყვილების რაოდენობა: ${memoryGame.numPairsFound}")
            val color = ArgbEvaluator().evaluate(
                memoryGame.numPairsFound.toFloat() / boardSize.getNumPairs(),
                ContextCompat.getColor(this, R.color.color_progress_none),
                ContextCompat.getColor(this, R.color.color_progress_full)

            ) as Int
            tvNumPairs.setTextColor(color)
            tvNumPairs.text ="წყვილი: ${memoryGame.numPairsFound} / ${boardSize.getNumPairs()}"
            if (memoryGame.haveWonGame()) {
                Snackbar.make(clRoot,"შენ გაიმარჯვე! გილოცავ.", Snackbar.LENGTH_LONG).show()
            }
        }
        tvNumMoves.text= "მცდელობა: ${memoryGame.getNumMoves()}"
        adapter.notifyDataSetChanged()
    }


}