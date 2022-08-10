package com.example.homeworklogapp

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.beust.klaxon.JsonReader
import com.beust.klaxon.Klaxon
import com.example.homeworklogapp.databinding.FragmentTodoBinding
import java.io.File
import java.io.StringReader

class FragmentTodo : Fragment() {

    private fun swipeFunctions() {
        ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.RIGHT) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                // this method is called
                // when the item is moved.
                return false
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                // this method is called when item is swiped.
                // below line is to remove item from our array list.
                taskList.removeAt(viewHolder.adapterPosition)

                // below line is to notify our item is removed from adapter.
                RVAdapter.notifyItemRemoved(viewHolder.adapterPosition)
            }
            // at last we are adding this
            // to our recycler view.
        }).attachToRecyclerView(RVTodo)
    }

    private fun createRV() {
        RVTodo = binding.rvTodo
        taskList = ArrayList()
        RVAdapter = RVAdapter(taskList)

        // set adapter to recycler view
        RVTodo.adapter = RVAdapter

        // adding data to list
        readJson()

        RVAdapter.notifyDataSetChanged()
    }

    private fun readJson() {

        val files = requireContext().fileList()
        val numFiles = files.size

        if (numFiles > 1) { // if "fileAssignment" exists, since files[0] is a default-added file

            // check if file exists
            val file = File(requireContext().filesDir, "fileAssignment")

            // * deserialize and read .json *
            // read json file
            val fileJson = file.readText()

            // convert fileJson into listPerson: List
            JsonReader(StringReader(fileJson)).use { reader ->
                reader.beginArray {
                    while (reader.hasNext()) {
                        val t = Klaxon().parse<Task>(reader)
                        taskList.add(t!!)
                    }
                }
            }
        }
    }

    lateinit var RVTodo: RecyclerView
    lateinit var RVAdapter: RVAdapter
    lateinit var taskList: ArrayList<Task>

    private var _binding: FragmentTodoBinding? = null

    // setup view binding
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentTodoBinding.inflate(inflater, container, false)
        return binding.root

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // create recycler view
        createRV()

        // fabAddTask
        binding.fabAddTask.setOnClickListener() {
            val intent = Intent(activity, ActivityAddTask::class.java)
            activity?.startActivity(intent)
        }

        // swipe functions
        swipeFunctions()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}