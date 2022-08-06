package com.example.homeworklogapp

import android.content.Context
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import androidx.lifecycle.Lifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter

const val totalTabs = 2

class MyAdapter(fragmentManager: FragmentManager, lifecycle: Lifecycle) :
        FragmentStateAdapter(fragmentManager, lifecycle) {

            override fun getItemCount(): Int {
                return totalTabs
            }

            override fun createFragment(position: Int): Fragment {
                when (position) {
                    0 -> return Fragment1()
                    1 -> return Fragment2()
                }

                return Fragment1()
            }
        }