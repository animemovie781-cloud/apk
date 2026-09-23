package com.tom.rv2ide.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.tom.rv2ide.R

class StoreFragment : BaseFragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val textView = TextView(requireContext())
        textView.text = "Store Coming Soon"
        textView.textSize = 24f
        textView.gravity = android.view.Gravity.CENTER
        return textView
    }
}
