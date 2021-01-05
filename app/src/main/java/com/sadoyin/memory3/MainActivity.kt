package com.sadoyin.memory3

import android.animation.ArgbEvaluator
import android.app.Activity
import android.content.Intent
import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.EditText
import android.widget.RadioGroup
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.github.jinatonic.confetti.CommonConfetti
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.firestore.FirebaseFirestore
import com.sadoyin.memory3.models.BoardSize
import com.sadoyin.memory3.models.MemoryGame
import com.sadoyin.memory3.models.UserImageList
import com.sadoyin.memory3.utils.EXTRA_BOARD_SIZE
import com.sadoyin.memory3.utils.EXTRA_GAME_NAME
import com.squareup.picasso.Picasso

class MainActivity : AppCompatActivity() {

    companion object{
        private const val TAG="MainActivity"
        private const val CREATE_REQUEST_CODE =1995
    }

    private var gameName: String? =null
    private var customGameImages: List<String>? =null
    private lateinit var clRoot:CoordinatorLayout
    private  lateinit var rvBoard :RecyclerView
    private lateinit var  tvNumMoves: TextView
    private lateinit var  tvNumPairs: TextView

    private val db =FirebaseFirestore.getInstance()
    private val gametName:String? =null
    private lateinit var memoryGame: MemoryGame
    private lateinit var adapter:MemoryBoardAdapter
    private var boardSize: BoardSize =BoardSize.EASY

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        clRoot =findViewById(R.id.clRoot)
        rvBoard = findViewById(R.id.rvBoard)
        tvNumMoves = findViewById(R.id.tvNumMoves)
        tvNumPairs = findViewById(R.id.tvNumPairs)

        setUpBoard()

    }


    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main,menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {

        when(item.itemId){
            R.id.mi_refresh->{
                if (memoryGame.getNumMoves()>0 && !memoryGame.haveWonGame()){
                    showAlertDialog("Quit your current game",null,View.OnClickListener {
                        setUpBoard()
                    })
                }else{
                    setUpBoard()
                }
                return true
            }

            R.id.mi_new_size->{
                showNewSizeDialog()
                return true
            }

            R.id.mi_custom ->{
                showCreationDialog()
                return true
            }

            R.id.mi_download->{
                showDownloadDialog()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode== CREATE_REQUEST_CODE && resultCode ==Activity.RESULT_OK){
            val customGame =data?.getStringExtra(EXTRA_GAME_NAME)
            if (customGame==null){
                return
            }
            downloadGame(customGame)
        }

        super.onActivityResult(requestCode, resultCode, data)
    }

    private fun showDownloadDialog() {
        val boardDownloadView = LayoutInflater.from(this).inflate(R.layout.dialog_download_board,null)
        showAlertDialog("Fetch Memory Game",boardDownloadView, View.OnClickListener {
            val etDownloadGame = boardDownloadView.findViewById<EditText>(R.id.etDownloadGame)
            val gameToDownload = etDownloadGame.text.toString().trim()
            downloadGame(gameToDownload)
        })
    }
    private fun downloadGame(customGameName: String) {
        db.collection("games").document(customGameName).get().addOnSuccessListener {document ->
            val userImageList =document.toObject(UserImageList::class.java)
            if (userImageList?.images==null){
                Snackbar.make(clRoot,"Sorry, we couldn't find any such game '$customGameName'",Snackbar.LENGTH_LONG).show()
                return@addOnSuccessListener
            }

            val numCards = userImageList.images.size * 2
            boardSize = BoardSize.getByValue(numCards)
            customGameImages =userImageList.images
            for (imageUrl in userImageList.images){
                Picasso.get().load(imageUrl).placeholder(R.drawable.ic_image).fetch()
            }
            Snackbar.make(clRoot,"You are now Playing '$customGameName'!", Snackbar.LENGTH_LONG).show()
            gameName =customGameName
            setUpBoard()

        }.addOnFailureListener{exception->

        }
    }

    private fun showCreationDialog() {
        val boardSizeView =LayoutInflater.from(this).inflate(R.layout.dialog_board_size,null)
        val radioGroupSize =boardSizeView.findViewById<RadioGroup>(R.id.radioGroup)


        showAlertDialog("Create your own memory board",boardSizeView, View.OnClickListener {
             val desiredBoardSize = when(radioGroupSize.checkedRadioButtonId){
                R.id.rbEasy->BoardSize.EASY
                R.id.rbMedium->BoardSize.MEDIUM
                else -> BoardSize.HARD
            }
            val intent = Intent(this,CreateActivity::class.java)
            intent.putExtra(EXTRA_BOARD_SIZE,desiredBoardSize)
            startActivityForResult(intent, CREATE_REQUEST_CODE)
        })
    }

    private fun showNewSizeDialog() {
        val boardSizeView =LayoutInflater.from(this).inflate(R.layout.dialog_board_size,null)
        val radioGroupSize =boardSizeView.findViewById<RadioGroup>(R.id.radioGroup)
        when(boardSize){
            BoardSize.EASY->radioGroupSize.check(R.id.rbEasy)
            BoardSize.MEDIUM -> radioGroupSize.check(R.id.rbMedium)
            BoardSize.HARD->radioGroupSize.check(R.id.rbHard)
        }
        showAlertDialog("Choose new size",boardSizeView, View.OnClickListener {
            boardSize = when(radioGroupSize.checkedRadioButtonId){
                R.id.rbEasy->BoardSize.EASY
                R.id.rbMedium->BoardSize.MEDIUM
                else -> BoardSize.HARD
            }

            gameName =null
            customGameImages=null
            setUpBoard()
        })
    }

    private fun showAlertDialog(title:String, view: View?,positiveClickListener:View.OnClickListener) {
        AlertDialog.Builder(this)
            .setTitle(title)
            .setView(view)
            .setNegativeButton("Cancel",null)
            .setPositiveButton("Ok"){_,_ ->
                positiveClickListener.onClick(null)
            }.show()

    }

    private fun setUpBoard() {

        supportActionBar?.title =gameName?:getString(R.string.app_name)
        when(boardSize){
            BoardSize.EASY->{
                tvNumMoves.text ="Easy:4x2"
                tvNumPairs.text ="Pairs:0/4"
            }

            BoardSize.MEDIUM->{
                tvNumMoves.text ="Medium:6x3"
                tvNumPairs.text ="Pairs:0/9"
            }

            BoardSize.HARD ->{
                tvNumMoves.text ="Hard:6x6"
                tvNumPairs.text ="Pairs:0/12"
            }
        }

        tvNumPairs.setTextColor(ContextCompat.getColor(this,R.color.colo_progress_none))
        memoryGame = MemoryGame(boardSize,customGameImages)
        adapter=MemoryBoardAdapter(this,boardSize, memoryGame.cards, object :MemoryBoardAdapter.CardClickListener{

            override fun onCardClicked(position: Int) {
                updateGameWithFlip(position)
            }

        })

        rvBoard.adapter=adapter
        rvBoard.setHasFixedSize(true)
        rvBoard.layoutManager = GridLayoutManager(this,boardSize.getWidth())
    }

    private fun updateGameWithFlip(position: Int) {

        if (memoryGame.haveWonGame()){
            Snackbar.make(clRoot,"You already won!",Snackbar.LENGTH_LONG).show()

            return
        }

        if (memoryGame.isCardFaceUp(position)){

            Snackbar.make(clRoot,"Invalid Move!",Snackbar.LENGTH_SHORT).show()
            return
        }
        if( memoryGame.flipCard(position)){

            val color =ArgbEvaluator().evaluate(
                memoryGame.numPairsFound.toFloat()/boardSize.getNumPairs(),
                ContextCompat.getColor(this,R.color.colo_progress_none),
                ContextCompat.getColor(this,R.color.colo_progress_full)
            ) as Int
            tvNumPairs.setTextColor(color)
            tvNumPairs.text ="Pairs ${memoryGame.numPairsFound} / ${boardSize.getNumPairs()}"
            if (memoryGame.haveWonGame()){
                Snackbar.make(clRoot,"You won! Congratulations",Snackbar.LENGTH_LONG).show()
                CommonConfetti.rainingConfetti(clRoot, intArrayOf(Color.RED,Color.CYAN,Color.BLUE,Color.DKGRAY)).oneShot()
            }
        }

        tvNumMoves.text = "Moves ${memoryGame.getNumMoves()}"

        adapter.notifyDataSetChanged()

    }
}
