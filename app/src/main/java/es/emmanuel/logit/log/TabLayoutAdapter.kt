package es.emmanuel.logit.log

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter

const val totalTabs = 2

class TabLayoutAdapter(fragmentManager: FragmentManager, lifecycle: Lifecycle) :
        FragmentStateAdapter(fragmentManager, lifecycle) {

            override fun getItemCount(): Int {
                return totalTabs
            }

            override fun createFragment(position: Int): Fragment {

                when (position) {
                    0 -> return TodoFragment()
                }

                return DoneFragment()
            }
        }