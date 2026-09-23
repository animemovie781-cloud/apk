package com.tom.rv2ide.fragments

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.tom.rv2ide.R
import java.io.File

class ProjectsAdapter(
    private val onOpenProject: (File) -> Unit,
    private val onBuildProject: (File) -> Unit,
    private val onSettingsClick: (File) -> Unit,
    private val onDeleteClick: (File) -> Unit
) : RecyclerView.Adapter<ProjectsAdapter.ProjectViewHolder>() {

    private var projects = listOf<File>()
    private var expandedPosition = -1

    fun submitList(newList: List<File>) {
        projects = newList
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProjectViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_project_home, parent, false)
        return ProjectViewHolder(view)
    }

    override fun onBindViewHolder(holder: ProjectViewHolder, position: Int) {
        val project = projects[position]
        val isExpanded = position == expandedPosition
        
        holder.bind(project, isExpanded)
        
        holder.mainContent.setOnClickListener {
            // Toggle expansion
            val prevExpanded = expandedPosition
            expandedPosition = if (isExpanded) -1 else position
            
            if (prevExpanded != -1) {
                notifyItemChanged(prevExpanded)
            }
            if (expandedPosition != -1) {
                notifyItemChanged(expandedPosition)
            }
        }
        
        // Double click or explicit "Open" could trigger this, for now let's say 
        // the main content expands, and there's a separate way to open.
        // Actually, Sketchware opens on click. Let's make long-click expand.
        
        holder.mainContent.setOnLongClickListener {
            val prevExpanded = expandedPosition
            expandedPosition = if (isExpanded) -1 else position
            
            if (prevExpanded != -1) notifyItemChanged(prevExpanded)
            if (expandedPosition != -1) notifyItemChanged(expandedPosition)
            true
        }
        
        // If clicking opens it:
        holder.mainContent.setOnClickListener {
            onOpenProject(project)
        }
        
        holder.expandIcon.setOnClickListener {
            val prevExpanded = expandedPosition
            expandedPosition = if (isExpanded) -1 else position
            
            if (prevExpanded != -1) notifyItemChanged(prevExpanded)
            if (expandedPosition != -1) notifyItemChanged(expandedPosition)
        }
        
        holder.actionBuild.setOnClickListener { onBuildProject(project) }
        holder.actionSettings.setOnClickListener { onSettingsClick(project) }
        holder.actionDelete.setOnClickListener { onDeleteClick(project) }
    }

    override fun getItemCount() = projects.size

    class ProjectViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val mainContent: LinearLayout = itemView.findViewById(R.id.main_content)
        val projectName: TextView = itemView.findViewById(R.id.project_name)
        val projectPackage: TextView = itemView.findViewById(R.id.project_package)
        val expandIcon: ImageView = itemView.findViewById(R.id.expand_icon)
        
        val expandedActions: LinearLayout = itemView.findViewById(R.id.expanded_actions)
        val actionBuild: View = itemView.findViewById(R.id.action_build)
        val actionSettings: View = itemView.findViewById(R.id.action_settings)
        val actionDelete: View = itemView.findViewById(R.id.action_delete)

        fun bind(project: File, isExpanded: Boolean) {
            projectName.text = project.name
            
            // In a real implementation we'd read gradle files for package and version
            projectPackage.text = project.absolutePath
            
            expandedActions.visibility = if (isExpanded) View.VISIBLE else View.GONE
            
            val iconRes = if (isExpanded) R.drawable.ic_expand_less else R.drawable.ic_expand_more
            expandIcon.setImageResource(iconRes)
        }
    }
}
