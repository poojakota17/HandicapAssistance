package com.cmpe295.iAssist.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.cmpe295.iAssist.R

class VisualAssistanceFragment : Fragment() {


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root: View = inflater.inflate(R.layout.fragment_visual, container, false)
        val textView: TextView = root.findViewById(R.id.text_home)
            textView.text = "Visual Assistance"
        return root
    }
}