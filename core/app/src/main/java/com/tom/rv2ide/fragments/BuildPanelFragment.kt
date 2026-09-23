package com.tom.rv2ide.fragments

import android.content.Intent
import android.os.Bundle
import android.os.SystemClock
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.content.FileProvider
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.tom.rv2ide.R
import com.tom.rv2ide.build.lightweight.BuildManager
import com.tom.rv2ide.build.lightweight.BuildState
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.io.File
import java.util.concurrent.TimeUnit

class BuildPanelFragment : DialogFragment() {

    private lateinit var btnClose: ImageView
    private lateinit var tvTitle: TextView
    private lateinit var btnCancel: MaterialButton
    private lateinit var progressSpinner: ProgressBar
    private lateinit var statusIcon: ImageView
    private lateinit var tvStatus: TextView
    private lateinit var tvTime: TextView
    private lateinit var recyclerLogs: RecyclerView
    private lateinit var postBuildActions: LinearLayout
    private lateinit var btnShare: MaterialButton
    private lateinit var btnInstall: MaterialButton
    
    private val logAdapter = BuildLogAdapter()
    private var startTimeMs: Long = 0
    private var isFinished = false
    private var outputApkFile: File? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.Theme_AppCompat_DayNight_NoActionBar)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_build_panel, container, false)
        
        btnClose = view.findViewById(R.id.btn_close)
        tvTitle = view.findViewById(R.id.tv_title)
        btnCancel = view.findViewById(R.id.btn_cancel)
        progressSpinner = view.findViewById(R.id.progress_spinner)
        statusIcon = view.findViewById(R.id.status_icon)
        tvStatus = view.findViewById(R.id.tv_status)
        tvTime = view.findViewById(R.id.tv_time)
        recyclerLogs = view.findViewById(R.id.recycler_logs)
        postBuildActions = view.findViewById(R.id.post_build_actions)
        btnShare = view.findViewById(R.id.btn_share)
        btnInstall = view.findViewById(R.id.btn_install)
        
        recyclerLogs.layoutManager = LinearLayoutManager(requireContext())
        recyclerLogs.adapter = logAdapter
        
        setupListeners()
        startTimer()
        observeBuild()
        
        return view
    }
    
    private fun setupListeners() {
        btnClose.setOnClickListener { dismiss() }
        btnCancel.setOnClickListener {
            BuildManager.cancelBuild()
            btnCancel.isEnabled = false
            btnCancel.text = "Cancelling..."
        }
        
        btnInstall.setOnClickListener {
            outputApkFile?.let { installApk(it) }
        }
        
        btnShare.setOnClickListener {
            outputApkFile?.let { shareApk(it) }
        }
    }
    
    private fun startTimer() {
        startTimeMs = SystemClock.elapsedRealtime()
        lifecycleScope.launch {
            while (!isFinished) {
                val elapsed = SystemClock.elapsedRealtime() - startTimeMs
                val minutes = TimeUnit.MILLISECONDS.toMinutes(elapsed)
                val seconds = TimeUnit.MILLISECONDS.toSeconds(elapsed) % 60
                tvTime.text = String.format("%02d:%02d", minutes, seconds)
                kotlinx.coroutines.delay(1000)
            }
        }
    }
    
    private fun observeBuild() {
        val projectManager = (requireActivity() as? com.tom.rv2ide.activities.MainActivity)?.projectManager
            ?: return // Need a real project manager instance
            
        lifecycleScope.launch {
            BuildManager.buildDebug(projectManager).collectLatest { progress ->
                tvStatus.text = progress.message
                
                // Add new logs
                logAdapter.submitList(BuildManager.getBuildLogs())
                if (logAdapter.itemCount > 0) {
                    recyclerLogs.scrollToPosition(logAdapter.itemCount - 1)
                }
                
                if (progress.isComplete) {
                    isFinished = true
                    progressSpinner.visibility = View.GONE
                    statusIcon.visibility = View.VISIBLE
                    btnCancel.visibility = View.GONE
                    
                    if (progress.state == BuildState.SUCCESS) {
                        statusIcon.setImageResource(R.drawable.ic_check_circle)
                        statusIcon.setColorFilter(0xFF4CAF50.toInt()) // Green
                        tvTitle.text = "Build Successful"
                        
                        outputApkFile = progress.result?.apkPath
                        postBuildActions.visibility = View.VISIBLE
                    } else if (progress.state == BuildState.CANCELLED) {
                        statusIcon.setImageResource(R.drawable.ic_close)
                        statusIcon.setColorFilter(0xFF9E9E9E.toInt()) // Grey
                        tvTitle.text = "Build Cancelled"
                    } else {
                        statusIcon.setImageResource(R.drawable.ic_close)
                        statusIcon.setColorFilter(0xFFFF5252.toInt()) // Red
                        tvTitle.text = "Build Failed"
                    }
                }
            }
        }
    }
    
    private fun installApk(file: File) {
        // Implement package installer intent
    }
    
    private fun shareApk(file: File) {
        // Implement share intent
    }
}

class BuildLogAdapter : RecyclerView.Adapter<BuildLogAdapter.LogViewHolder>() {
    private var logs = listOf<String>()

    fun submitList(newLogs: List<String>) {
        logs = newLogs
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LogViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_build_log, parent, false)
        return LogViewHolder(view as TextView)
    }

    override fun onBindViewHolder(holder: LogViewHolder, position: Int) {
        val text = logs[position]
        holder.textView.text = text
        
        when {
            text.contains("[ERROR]") -> holder.textView.setTextColor(0xFFFF5252.toInt())
            text.contains("[WARN]") -> holder.textView.setTextColor(0xFFFFC107.toInt())
            else -> holder.textView.setTextColor(0xFFA8ABB5.toInt())
        }
    }

    override fun getItemCount() = logs.size

    class LogViewHolder(val textView: TextView) : RecyclerView.ViewHolder(textView)
}
