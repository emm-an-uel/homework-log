package es.emmanuel.logit.calendar

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager.widget.PagerAdapter
import com.example.logit.R
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.shashank.sony.fancytoastlib.FancyToast
import es.emmanuel.logit.ParentActivity
import es.emmanuel.logit.Task
import java.time.temporal.ChronoUnit
import java.util.*

class CalendarPagerAdapter(
    private val context: Context,
    private val todoList: List<Task>,
    private val doneList: List<Task>,
    private val mapOfTasks: Map<Int, ArrayList<Task>>,
    private val minDate: Calendar,
    private val maxDate: Calendar,
    private val selectedDate: Calendar,
    private val mapSubjectColor: Map<String, Int>,
    private val colors: List<Int>,
    private val showCompletedTasks: Boolean
) : PagerAdapter() {

    private val initialPosition = ChronoUnit.DAYS.between(minDate.toInstant(), selectedDate.toInstant()).toInt()
    // number of days between minDate and selectedDate to determine ViewPager's initial position

    private val initialPageAndDate = Pair<Int, Calendar>(initialPosition, selectedDate)

    override fun getCount(): Int {
        return ChronoUnit.DAYS.between(minDate.toInstant(), maxDate.toInstant()).toInt() // total number of days between minDate and maxDate
    }

    override fun isViewFromObject(view: View, `object`: Any): Boolean {
        return view == `object`
    }

    override fun instantiateItem(container: ViewGroup, position: Int): Any {

        val currentDate: Calendar = initialPageAndDate.second.clone() as Calendar
        currentDate.add(Calendar.DATE, position - initialPageAndDate.first) // adds the number of days it is away from the initialDate
        val currentDateInt = calendarToInt(currentDate)

        val view = LayoutInflater.from(context).inflate(R.layout.calendar_card_item, container, false)
        view.tag = position // tag for adjustments of size and opacity in CalendarDialog.updatePager

        val tvDayOfMonth: TextView = view.findViewById(R.id.tvDayOfMonth)
        val tvDayOfWeek: TextView = view.findViewById(R.id.tvDayOfWeek)
        val tvNoEvents: TextView = view.findViewById(R.id.tvNoEvents)
        val rvEvents: RecyclerView = view.findViewById(R.id.rvEvents)
        val fabAddTask: FloatingActionButton = view.findViewById(R.id.fabNewTask)

        tvDayOfMonth.text = currentDate.get(Calendar.DAY_OF_MONTH).toString()
        tvDayOfWeek.text = getDayOfWeek(currentDate.get(Calendar.DAY_OF_WEEK))

        // fabAddTask functionality
        if (currentDateInt < calendarToInt(Calendar.getInstance())) { // currentDate is in the past
            fabAddTask.visibility = View.GONE
        } else {
            fabAddTask.setOnClickListener { // calls method in ParentActivity since PagerAdapter has no property 'listSubjects'
                (context as ParentActivity).createNewTask(calendarToString(currentDate))
            }
        }

        // show today's tasks
        var hasEvents = false
        if (mapOfTasks.containsKey(currentDateInt)) { // if there are tasks due today

            val todayTasks = if (!showCompletedTasks) { // contents of 'todayTasks' depends on whether showCompletedTasks
                removeCompletedTasks(mapOfTasks[currentDateInt]!!)
            } else {
                mapOfTasks[currentDateInt]!!
            }
            val rvAdapter = CalendarRVAdapter(todayTasks, mapSubjectColor, colors, showCompletedTasks)
            rvEvents.adapter = rvAdapter

            // click listener to watch for changes in 'completed' status
            rvAdapter.setOnItemClickListener(object: CalendarRVAdapter.OnItemClickListener {
                override fun onItemClick(position: Int) {
                    val task = todayTasks[position]
                    if (task.completed) { // task currently complete, to mark as undone
                        val actualPosition: Int? = findPositionDone(task) // find task in doneList
                        if (actualPosition != null) {
                            (context as ParentActivity).viewModel.markAsUndone(task)
                        } else { // if failed to find actualPosition
                            FancyToast.makeText(context, "Error: Could not mark as undone", FancyToast.LENGTH_SHORT, FancyToast.DEFAULT, false).show()
                        }

                    } else { // task currently incomplete, to mark as done
                        val actualPosition: Int? = findPositionTodo(task) // find task in todoList
                        if (actualPosition != null) {
                            (context as ParentActivity).viewModel.markAsDone(task)
                        } else {
                            FancyToast.makeText(context, "Error: Could not mark as done", FancyToast.LENGTH_SHORT, FancyToast.DEFAULT, false).show()
                        }
                    }
                }
            })

            // show/hide fab on scroll
            rvEvents.addOnScrollListener(object: RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy)
                    if (dy > 0) { // scrolling down - fab is shown
                        fabAddTask.hide()
                    } else { // scrolling up - fab is not shown
                        fabAddTask.show()
                    }
                }
            })

            hasEvents = true
        }

        if (!hasEvents) { // no events for the day
            rvEvents.visibility = View.GONE
            tvNoEvents.visibility = View.VISIBLE
        }

        container.addView(view)
        return view
    }

    private fun removeCompletedTasks(tasks: ArrayList<Task>): ArrayList<Task> {
        val updatedList = arrayListOf<Task>()
        for (t in tasks) {
            if (!t.completed) {
                updatedList.add(t)
            }
        }
        return updatedList
    }

    private fun findPositionTodo(task1: Task): Int? {
        for (task in todoList) {
            if (task.id == task1.id) {
                return todoList.indexOf(task)
            }
        }
        return null
    }

    private fun findPositionDone(task1: Task): Int? {
        for (task in doneList) {
            if (task.id == task1.id) {
                return doneList.indexOf(task)
            }
        }
        return null
    }

    private fun getDayOfWeek(dayInt: Int): String {
        return when (dayInt) {
            2 -> "Monday"
            3 -> "Tuesday"
            4 -> "Wednesday"
            5 -> "Thursday"
            6 -> "Friday"
            7 -> "Saturday"
            else -> "Sunday"
        }
    }

    override fun destroyItem(container: ViewGroup, position: Int, `object`: Any) {
        container.removeView(`object` as View)
    }

    override fun getItemPosition(`object`: Any): Int {
        return POSITION_NONE // not sure what this does but it was included in the original CalendarView-Widget app by hugomfandrade
    }

    private fun calendarToInt(date: Calendar): Int {
        val year = date.get(Calendar.YEAR)
        val month = date.get(Calendar.MONTH)+1
        val day = date.get(Calendar.DAY_OF_MONTH)

        var monthString = month.toString()
        var dayString = day.toString()

        // ensure proper MM format
        if (month < 10) {
            monthString = "0$month" // eg convert "8" to "08"
        }

        // ensure proper DD format
        if (day < 10) {
            dayString = "0$day"
        }

        // convert to YYYYMMDD format
        val dateString = "$year$monthString$dayString"
        return (dateString.toInt())
    }

    private fun calendarToString(calendar: Calendar): String {
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)+1
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        var monthString = month.toString()
        var dayString = day.toString()

        // ensure proper MM format
        if (month < 10) {
            monthString = "0$month" // eg convert "8" to "08"
        }

        // ensure proper DD format
        if (day < 10) {
            dayString = "0$day"
        }

        // convert to DD MM YYYY format
        return "$dayString $monthString $year"
    }
}