package com.example.taskapp

import android.app.AlarmManager
import android.app.AlertDialog
import android.app.PendingIntent
import android.content.Intent
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v7.app.AppCompatActivity;
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.SearchView
import io.realm.Realm
import io.realm.RealmChangeListener
import io.realm.Sort

import kotlinx.android.synthetic.main.activity_main.*
import java.util.*


const val EXTRA_TASK = "com.example.taskapp.TASK"
class MainActivity : AppCompatActivity(), android.support.v7.widget.SearchView.OnQueryTextListener {

    private lateinit var mRealm: Realm

    private val mRealmListener = object : RealmChangeListener<Realm> {
        override fun onChange(element: Realm) {
            reloadListView()
        }
    }

    private lateinit var mTaskAdapter: TaskAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        fab.setOnClickListener { view ->
            val intent = Intent(this@MainActivity, InputActivity::class.java)
            startActivity(intent)
        }

        searchView.setOnQueryTextListener(this)
        searchView.setIconified(false);
        searchView.setQueryHint("カテゴリで検索します")
        // Realmの設定
        mRealm = Realm.getDefaultInstance()
        mRealm.addChangeListener(mRealmListener)

        // ListViewの設定
        mTaskAdapter = TaskAdapter(this@MainActivity)

        // ListViewをタップしたときの処理
        listView1.setOnItemClickListener { parent, view, position, id ->
            // 入力・編集する画面に遷移させる
            val task = parent.adapter.getItem(position) as Task
            val intent = Intent(this@MainActivity, InputActivity::class.java)
            intent.putExtra(EXTRA_TASK, task.id)
            startActivity(intent)

        }

        // ListViewを長押ししたときの処理
        listView1.setOnItemLongClickListener { parent, view, position, id ->

            val task = parent.adapter.getItem(position) as Task

            // ダイアログを表示する
            val builder = AlertDialog.Builder(this@MainActivity)

            builder.setTitle("削除")
            builder.setMessage(task.title + "を削除しますか")

            builder.setPositiveButton("OK"){_, _ ->
                val results = mRealm.where(Task::class.java).equalTo("id", task.id).findAll()

                mRealm.beginTransaction()
                results.deleteAllFromRealm()
                mRealm.commitTransaction()
                val resultIntent = Intent(applicationContext, TaskAlarmReceiver::class.java)
                val resultPendingIntent = PendingIntent.getBroadcast(
                    this@MainActivity,
                    task.id,
                    resultIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT
                )

                val alarmManager = getSystemService(ALARM_SERVICE) as AlarmManager
                alarmManager.cancel(resultPendingIntent)
                reloadListView()
            }

            builder.setNegativeButton("CANCEL", null)

            val dialog = builder.create()
            dialog.show()
            true
        }
        reloadListView()
    }

    private fun reloadListView() {
        val taskRealmResults = mRealm.where(Task::class.java).findAll().sort("date", Sort.DESCENDING)
        mTaskAdapter.taskList = mRealm.copyFromRealm(taskRealmResults)
        listView1.adapter = mTaskAdapter
        mTaskAdapter.notifyDataSetChanged()
    }

    override fun onDestroy() {
        super.onDestroy()

        mRealm.close()
    }

    override fun onQueryTextSubmit(p0: String?): Boolean {

            val results = mRealm.where(Task::class.java).equalTo("category", p0).findAll()
            mTaskAdapter.taskList = results
            listView1.adapter = mTaskAdapter
            mTaskAdapter.notifyDataSetChanged()
            return true
    }

    override fun onQueryTextChange(p0: String?): Boolean {
        if (p0!!.count() > 0) {
            val results = mRealm.where(Task::class.java).beginsWith("category", p0).findAll()
            mTaskAdapter.taskList = results
            listView1.adapter = mTaskAdapter
            mTaskAdapter.notifyDataSetChanged()

        }else{
            reloadListView()

        }

        return true
    }

}


















