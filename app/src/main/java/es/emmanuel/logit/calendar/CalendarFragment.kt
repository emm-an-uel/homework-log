package es.emmanuel.logit.calendar

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.TypedValue
import android.view.*
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.viewpager.widget.PagerAdapter
import androidx.viewpager.widget.ViewPager
import com.example.logit.R
import com.example.logit.databinding.FragmentCalendarBinding
import es.emmanuel.logit.Task
import es.emmanuel.logit.ViewModelParent
import es.emmanuel.logit.addtask.AddTaskActivity
import es.emmanuel.logit.settings.SettingsItem
import org.hugoandrade.calendarviewlib.CalendarView
import java.text.DateFormatSymbols
import java.time.temporal.ChronoUnit
import java.util.*

class CalendarFragment : Fragment() {

    private var selectedDate: Calendar = Calendar.getInstance()

    private var _binding: FragmentCalendarBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: ViewModelParent
    private lateinit var calDialogView: View
    private lateinit var viewPagerAdapter: PagerAdapter
    private lateinit var viewPager: ViewPager
    private lateinit var mAlertDialog: AlertDialog

    private lateinit var mapOfTasks: Map<Int, ArrayList<Task>>
    private lateinit var minDate: Calendar
    private lateinit var maxDate: Calendar
    private lateinit var todoList: List<Task>
    private lateinit var doneList: List<Task>
    private lateinit var combinedList: ArrayList<Task>
    private lateinit var colors: List<Int>
    private lateinit var mapSubjectColor: Map<String, Int>

    private lateinit var settings: List<SettingsItem>
    private var showCompletedTasks = false
    // TODO: future user preferences here //

    // for PagerAdapter swipe animations
    private val MIN_OFFSET = 0f
    private val MAX_OFFSET = 0.5f
    private val MIN_ALPHA = 0.5f
    private val MIN_SCALE = 0.8f
    private var totalPages = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel = ViewModelProvider(requireActivity())[ViewModelParent::class.java]
        setMinMaxDates()
        getData()
    }

    override fun onPause() {
        super.onPause()
        binding.fabAddTask.hide()
    }

    private fun getData() {
        mapOfTasks = viewModel.getMapOfTasks()
        todoList = viewModel.getTodoList()
        doneList = viewModel.getDoneList()
        combinedList = arrayListOf()
        combinedList.apply {
            addAll(todoList)
            addAll(doneList)
            sortWith(compareBy(Task::dueDateInt, Task::subject, Task::task))
        }
        colors = viewModel.getColors()
        mapSubjectColor = viewModel.getMapSubjectColor()
        settings = viewModel.getListSettings()

        showCompletedTasks = intToBoolean(settings[4].option)
    }

    override fun onResume() {
        super.onResume()

        binding.fabAddTask.show()
        getData()
        setupCalendar()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentCalendarBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        requireActivity().title = ""

        setupCalendar()
        setupMenu()

        binding.fabAddTask.setOnClickListener {
            createNewTask()
        }
    }

    private fun setupMenu() {
        requireActivity().addMenuProvider(object: MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.calendar_menu, menu)
                val rootView: View? = menu.findItem(R.id.today).actionView
                if (rootView != null) {
                    val tvDate: TextView = rootView.findViewById(R.id.tvDate)
                    tvDate.text = (Calendar.getInstance().get(Calendar.DAY_OF_MONTH)).toString() // update menu item text
                    rootView.setOnClickListener {
                        binding.calendarView.selectedDate = Calendar.getInstance() // updates CalendarView
                        selectedDate = Calendar.getInstance() // updates variable 'selectedDate'
                    }
                }
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                return false
            }

        }, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }

    private fun setMinMaxDates() {
        // minDate and maxDate will be passed to PagerAdapter
        // so it knows how many pages there are and what page it should be showing
        minDate = Calendar.getInstance()
        minDate.set(1992, 0, 1) // note: Calendar has months from 0 - 11
        maxDate = Calendar.getInstance()
        maxDate.set(2100, 0, 1)
        totalPages = ChronoUnit.DAYS.between(minDate.toInstant(), maxDate.toInstant()).toInt()
    }

    private fun setupCalendar() {
        // initial sync
        val currentMonth = binding.calendarView.shownMonth
        val currentYear = binding.calendarView.shownYear
        syncMonth(currentMonth, currentYear)

        // sync as user swipes through calendar
        binding.calendarView.setOnMonthChangedListener { month, year ->
            syncMonth(month, year)
        }

        addCalendarObjects() // add user events to calendar
        binding.calendarView.setOnItemClickedListener { calendarObjects, _, newSelectedDate ->
            if (calendarObjects.size > 0) { // if there are events
                showCalendarDialog(newSelectedDate)

            } else { // if no events that day
                // user has to click twice to create new task
                if (isSameDate(selectedDate, newSelectedDate)) {
                    createNewTask()
                } else {
                    selectedDate = newSelectedDate
                }
            }
        }
    }

    private fun isSameDate(date1: Calendar, date2: Calendar): Boolean {
        if (date1.get(Calendar.DAY_OF_MONTH) != date2.get(Calendar.DAY_OF_MONTH)) {
            return false
        }
        if (date1.get(Calendar.MONTH) != date2.get(Calendar.MONTH)) {
            return false
        }
        return date1.get(Calendar.YEAR) == date2.get(Calendar.YEAR)
    }

    private fun createNewTask() {
        val intent = Intent(requireContext(), AddTaskActivity::class.java)

        val listSubjects: ArrayList<String> = viewModel.getListSubjects()
        intent.putExtra("listSubjects", listSubjects)
        if (selectedDate > Calendar.getInstance()) { // this prevent user from choosing a dueDate before today's date
            intent.putExtra("selectedDate", calendarToString(selectedDate)) // pass selectedDate to AddTaskActivity
        }

        startActivity(intent)
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

    private fun addCalendarObjects() { // add CalendarObjects to CalendarView
        val calObjectList = arrayListOf<CalendarView.CalendarObject>()
        var id = 0
        if (showCompletedTasks) {
            for (task in combinedList) {
                if (!task.completed) { // task not completed - opaque color
                    val dueDate: Calendar = intToCalendar(task.dueDateInt)
                    dueDate.add(Calendar.SECOND, id) // adds seconds to the 'date' parameter so the order that CalendarObjects are
                    // displayed are synced with the order that Tasks are displayed in the CalendarDialog
                    val bgColorIndex = mapSubjectColor[task.subject]
                    val bgColor = if (bgColorIndex != null) {
                        ContextCompat.getColor(requireContext(), colors[bgColorIndex])
                    } else {
                        ContextCompat.getColor(requireContext(), R.color.gray)
                    }
                    calObjectList.add(
                        CalendarView.CalendarObject(
                            id.toString(),
                            dueDate,
                            bgColor,
                            ContextCompat.getColor(requireContext(), com.google.android.material.R.color.mtrl_btn_transparent_bg_color)
                        )
                    )
                } else { // task completed - translucent color
                    val dueDate: Calendar = intToCalendar(task.dueDateInt)
                    dueDate.add(Calendar.SECOND, id)
                    val bgColorIndex = mapSubjectColor[task.subject]
                    val bgColor = if (bgColorIndex != null) {
                        ContextCompat.getColor(requireContext(), colors[bgColorIndex])
                    } else {
                        ContextCompat.getColor(requireContext(), R.color.gray)
                    }
                    val bgColorTranslucent = ColorUtils.setAlphaComponent(bgColor, 100) // set alpha to make completed Tasks' CalendarObjects appear translucent
                    val checkedColorTranslucent = ColorUtils.setAlphaComponent(getColor(requireContext(), androidx.appcompat.R.attr.colorAccent), 130)
                    calObjectList.add(
                        CalendarView.CalendarObject(
                            id.toString(),
                            dueDate,
                            bgColorTranslucent,
                            checkedColorTranslucent
                        )
                    )
                }
                id++
            }

        } else { // !showCompletedTasks
            for (task in todoList) { // !completed - all opaque
                val dueDate: Calendar = intToCalendar(task.dueDateInt)
                dueDate.add(Calendar.SECOND, id)
                val bgColorIndex = mapSubjectColor[task.subject]
                val bgColor = if (bgColorIndex != null) {
                    ContextCompat.getColor(requireContext(), colors[bgColorIndex])
                } else {
                    ContextCompat.getColor(requireContext(), R.color.gray)
                }
                calObjectList.add(
                    CalendarView.CalendarObject(
                        id.toString(),
                        dueDate,
                        bgColor,
                        ContextCompat.getColor(requireContext(), com.google.android.material.R.color.mtrl_btn_transparent_bg_color)
                    )
                )
                id++
            }
        }

        binding.calendarView.setCalendarObjectList(calObjectList)
    }

    private fun syncMonth(currentMonth: Int, currentYear: Int) {
        // a custom Month header was used in this calendar
        // this syncs the text of the custom header with the month shown in the Calendar
        val month = DateFormatSymbols().months[currentMonth]
        binding.tvMonth.text = "$month $currentYear"
    }

    private fun showCalendarDialog(selectedDate: Calendar) {
        // inflate the view for the calendar dialog
        calDialogView = View.inflate(requireContext(), R.layout.calendar_dialog, null)

        // set up the ViewPager adapter
        viewPagerAdapter = CalendarPagerAdapter(requireContext(), todoList, doneList, mapOfTasks, minDate, maxDate, selectedDate, mapSubjectColor, colors, showCompletedTasks)

        val index = ChronoUnit.DAYS.between(minDate.toInstant(), selectedDate.toInstant()).toInt() // corresponding index for the current date

        viewPager = calDialogView.findViewById(R.id.viewPager)
        viewPager.apply {
            offscreenPageLimit = 3
            adapter = viewPagerAdapter
            currentItem = index
            setPadding(100, 0, 100, 0)
        }
        pagerSwipeAnimations()

        // display calDialogView in an AlertDialog
        mAlertDialog = AlertDialog.Builder(requireContext()).create()
        if (mAlertDialog.window != null) {
            mAlertDialog.window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        }
        mAlertDialog.apply {
            setCanceledOnTouchOutside(true)
            onPause()
            show()
            setContentView(calDialogView)
            setOnDismissListener {
                onResume()
            }
        }
    }

    private fun pagerSwipeAnimations() {
        viewPager.addOnPageChangeListener(object : ViewPager.OnPageChangeListener {
            override fun onPageScrolled(
                position: Int,
                positionOffset: Float,
                positionOffsetPixels: Int
            ) {
                // update view scale and alpha of views not currently focused

                adjustOpacity(viewPager.findViewWithTag(position), 1f - positionOffset) // current page
                if ((position + 1) < totalPages) { // next page
                    adjustOpacity(viewPager.findViewWithTag(position + 1), positionOffset)
                }
                if ((position + 2) < totalPages) { // two pages in advance
                    // (so it's already made smaller before user can see it - smoother look)
                    adjustOpacity(viewPager.findViewWithTag(position + 2), 0f)
                }
                if ((position - 1) >= 0) { // previous page
                    adjustOpacity(viewPager.findViewWithTag(position - 1), 0f)
                }
                if ((position - 2) >= 0) { // two pages before
                    adjustOpacity(viewPager.findViewWithTag(position - 2), 0f)
                }
            }

            override fun onPageSelected(position: Int) {
                // do nothing
            }

            override fun onPageScrollStateChanged(state: Int) {
                // do nothing
            }
        })
    }

    private fun adjustOpacity(view: View, offset: Float) {
        // this method adjusts the size and opacity of ViewPager views which aren't currently focused
        var adjustedOffset: Float =
            (1.0f - 0.0f) * (offset - MIN_OFFSET) / (MAX_OFFSET - MIN_OFFSET) + 0.0f
        adjustedOffset = if (adjustedOffset > 1f) 1f else adjustedOffset
        adjustedOffset = if (adjustedOffset < 0f) 0f else adjustedOffset

        val alpha: Float =
            adjustedOffset * (1f - MIN_ALPHA) + MIN_ALPHA
        val scale: Float =
            adjustedOffset * (1f - MIN_SCALE) + MIN_SCALE

        view.alpha = alpha
        view.scaleY = scale
    }

    private fun intToCalendar(int: Int): Calendar {
        val string = int.toString()
        val year = string.take(4).toInt()
        val monthDay = string.takeLast(4)
        val month = (monthDay.take(2).toInt() - 1) // Calendar months go from 0 to 11
        val day = monthDay.takeLast(2).toInt()

        val calendar = Calendar.getInstance()
        calendar.set(year, month, day)
        return calendar
    }

    private fun intToBoolean(option: Int): Boolean {
        return option != 0
    }

    private fun getColor(context: Context, colorResId: Int): Int {
        val typedValue = TypedValue()
        val typedArray = context.obtainStyledAttributes(typedValue.data, intArrayOf(colorResId))
        val color = typedArray.getColor(0, 0)
        typedArray.recycle()
        return color
    }
}
