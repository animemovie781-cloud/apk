package com.tom.rv2ide.fragments

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.PopupMenu
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.tom.rv2ide.R
import com.tom.rv2ide.activities.MainActivity
import com.tom.rv2ide.templates.preferences.WizardPreferences
import com.tom.rv2ide.utils.Environment
import com.tom.rv2ide.utils.GeneralFileUtils
import com.tom.rv2ide.build.lightweight.BuildManager
import com.tom.rv2ide.build.lightweight.BuildProgress
import com.tom.rv2ide.projects.ProjectManager
import java.io.File

class ProjectsHomeFragment : BaseFragment() {

    private lateinit var projectsRecycler: RecyclerView
    private lateinit var emptyStateText: TextView
    private lateinit var searchInput: EditText
    private lateinit var sortButton: ImageView
    private lateinit var fabNewProject: ExtendedFloatingActionButton
    private lateinit var restoreProjectsCard: MaterialCardView
    private lateinit var menuButton: ImageView
    
    private lateinit var adapter: ProjectsAdapter
    
    private var allProjects = listOf<File>()
    
    enum class SortOrder {
        RECENT, NAME_ASC, NAME_DESC
    }
    
    private var currentSort = SortOrder.RECENT

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_projects_home, container, false)
        
        projectsRecycler = view.findViewById(R.id.projects_recycler)
        emptyStateText = view.findViewById(R.id.empty_state_text)
        searchInput = view.findViewById(R.id.search_input)
        sortButton = view.findViewById(R.id.sort_button)
        fabNewProject = view.findViewById(R.id.fab_new_project)
        restoreProjectsCard = view.findViewById(R.id.restore_projects_card)
        menuButton = view.findViewById(R.id.menu_button)
        
        setupRecyclerView()
        setupListeners()
        loadProjects()
        
        return view
    }
    
    private fun setupRecyclerView() {
        adapter = ProjectsAdapter(
            onOpenProject = { file -> openProject(file) },
            onBuildProject = { file -> buildProject(file) },
            onSettingsClick = { file -> openProjectSettings(file) },
            onDeleteClick = { file -> deleteProject(file) }
        )
        projectsRecycler.layoutManager = LinearLayoutManager(requireContext())
        projectsRecycler.adapter = adapter
    }
    
    private fun setupListeners() {
        fabNewProject.setOnClickListener {
            val mainActivity = requireActivity() as? MainActivity
            // Trigger the ATC Wizard
            // This is a placeholder for the actual IDE's new project trigger
        }
        
        restoreProjectsCard.setOnClickListener {
            // Placeholder for restore projects intent
        }
        
        menuButton.setOnClickListener {
            val mainActivity = requireActivity() as? MainActivity
            // mainActivity?.openDrawer()
        }
        
        searchInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                filterProjects(s.toString())
            }
        })
        
        sortButton.setOnClickListener { view ->
            showSortMenu(view)
        }
    }
    
    private fun showSortMenu(anchor: View) {
        val popup = PopupMenu(requireContext(), anchor)
        popup.menu.add(0, 1, 0, "Recently modified")
        popup.menu.add(0, 2, 0, "Name (A-Z)")
        popup.menu.add(0, 3, 0, "Name (Z-A)")
        
        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                1 -> { currentSort = SortOrder.RECENT; applySortAndFilter() }
                2 -> { currentSort = SortOrder.NAME_ASC; applySortAndFilter() }
                3 -> { currentSort = SortOrder.NAME_DESC; applySortAndFilter() }
            }
            true
        }
        popup.show()
    }
    
    private fun loadProjects() {
        val projectDirProjects = GeneralFileUtils.listDirsInDirectory(Environment.PROJECTS_DIR)
            .filter { isValidAndroidProject(it) }

        val recentProjectPaths = WizardPreferences.getRecentProjects(requireContext())
        val recentProjectFiles = recentProjectPaths.mapNotNull { path ->
            val file = File(path)
            if (file.exists() && file.isDirectory && isValidAndroidProject(file)) file else null
        }

        val allProjectsMap = mutableMapOf<String, File>()
        recentProjectFiles.forEach { file -> allProjectsMap[file.absolutePath] = file }
        projectDirProjects.forEach { file -> allProjectsMap[file.absolutePath] = file }
        
        // Default sort by recent
        allProjects = allProjectsMap.values.toList().sortedWith(
            compareBy<File> { project ->
                val recentIndex = recentProjectPaths.indexOf(project.absolutePath)
                if (recentIndex >= 0) recentIndex else Int.MAX_VALUE
            }.thenByDescending { it.lastModified() }
        )
        
        applySortAndFilter()
    }
    
    private fun applySortAndFilter() {
        filterProjects(searchInput.text.toString())
    }
    
    private fun filterProjects(query: String) {
        var filtered = if (query.isBlank()) {
            allProjects
        } else {
            allProjects.filter { 
                it.name.contains(query, ignoreCase = true) 
                // A real implementation would parse gradle files to check app name/package
            }
        }
        
        filtered = when (currentSort) {
            SortOrder.RECENT -> filtered // Already sorted by recent in loadProjects if empty query, but good enough for now
            SortOrder.NAME_ASC -> filtered.sortedBy { it.name.lowercase() }
            SortOrder.NAME_DESC -> filtered.sortedByDescending { it.name.lowercase() }
        }
        
        adapter.submitList(filtered)
        
        if (filtered.isEmpty()) {
            projectsRecycler.visibility = View.GONE
            emptyStateText.visibility = View.VISIBLE
        } else {
            projectsRecycler.visibility = View.VISIBLE
            emptyStateText.visibility = View.GONE
        }
    }
    
    private fun isValidAndroidProject(dir: File): Boolean {
        // Simplified check: needs a build.gradle or app/build.gradle
        val hasRootGradle = File(dir, "build.gradle").exists() || File(dir, "build.gradle.kts").exists()
        val hasAppGradle = File(File(dir, "app"), "build.gradle").exists() || File(File(dir, "app"), "build.gradle.kts").exists()
        return hasRootGradle || hasAppGradle
    }
    
    private fun openProject(projectDir: File) {
        // Delegate to MainActivity
        // val mainActivity = requireActivity() as? MainActivity
        // mainActivity?.openProject(projectDir)
    }
    
    private fun buildProject(projectDir: File) {
        // Start the lightweight build using the BuildManager
        // A real implementation would instantiate a proper IProjectManager
        // BuildManager.buildDebug(ProjectManager(projectDir))
    }
    
    private fun openProjectSettings(projectDir: File) {
        // TODO: Show project settings dialog
    }
    
    private fun deleteProject(projectDir: File) {
        AlertDialog.Builder(requireContext())
            .setTitle("Delete Project")
            .setMessage("Are you sure you want to delete ${projectDir.name}? This cannot be undone.")
            .setPositiveButton("Delete") { _, _ ->
                projectDir.deleteRecursively()
                loadProjects()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
