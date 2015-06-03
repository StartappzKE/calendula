package es.usc.citius.servando.calendula.fragments;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.text.Html;
import android.text.Spanned;
import android.text.format.Time;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.NumberPicker;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import com.doomonafireball.betterpickers.numberpicker.NumberPickerBuilder;
import com.doomonafireball.betterpickers.numberpicker.NumberPickerDialogFragment;
import com.doomonafireball.betterpickers.radialtimepicker.RadialTimePickerDialog;
import com.doomonafireball.betterpickers.recurrencepicker.EventRecurrence;
import com.doomonafireball.betterpickers.recurrencepicker.RecurrencePickerDialog;
import com.google.ical.values.DateTimeValueImpl;
import com.google.ical.values.DateValue;
import com.google.ical.values.Frequency;
import es.usc.citius.servando.calendula.R;
import es.usc.citius.servando.calendula.persistence.RepetitionRule;
import es.usc.citius.servando.calendula.persistence.Routine;
import es.usc.citius.servando.calendula.persistence.Schedule;
import es.usc.citius.servando.calendula.persistence.ScheduleItem;
import es.usc.citius.servando.calendula.persistence.ScheduleItemComparator;
import es.usc.citius.servando.calendula.util.ScheduleHelper;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.joda.time.LocalTime;

/**
 * Created by joseangel.pineiro on 12/11/13.
 */
public class ScheduleTimetableFragment extends Fragment
    implements NumberPickerDialogFragment.NumberPickerDialogHandler,
    RecurrencePickerDialog.OnRecurrenceSetListener, RadialTimePickerDialog.OnTimeSetListener {

    public static final String TAG = ScheduleTimetableFragment.class.getName();

    public static final int REF_DIALOG_HOURLY_INTERVAL = 1;
    public static final int REF_DIALOG_ROUTINE_INTERVAL = 2;
    public static final int REF_DIALOG_CYCLE_DAYS = 3;
    public static final int REF_DIALOG_CYCLE_REST = 4;

    final Frequency[] FREQ =
        new Frequency[] { Frequency.DAILY, Frequency.WEEKLY, Frequency.MONTHLY };

    LinearLayout timetableContainer;
    int timesPerDay = 1;

    int cycleDays = -1;
    int cycleRest = -1;

    Spinner scheduleSpinner;
    Spinner repeatTypeSpinner;
    Spinner freqSpinner;
    ScheduleItemComparator scheduleItemComparator = new ScheduleItemComparator();

    Button buttonScheduleStart;
    Button buttonScheduleEnd;
    Button intervalEditText;
    Button hourlyIntervalEditText;
    Button hourlyIntervalFrom;
    Button hourlyIntervalRepeatDose;

    Button periodValue;
    Button periodRest;

    ImageButton cycleSpinnerTrigger;

    //RadioGroup scheduleTypeRadioGroup;

    ImageButton clearStartButton;
    ImageButton clearEndButton;

    View daySelectionBox;
    View customRepeatBox;
    TextView ruleText;

    Schedule schedule;
    boolean ignoreNextEvent = true;

    //View boxType;
    View boxHourlyInterval;
    View boxTimesByDay;
    View boxTimetable;
    View boxRepeat;
    View boxPeriod;
    View boxDuration;
    View boxHelp;

    TextView helpView;
    ImageButton nextButton;
    ScrollView scrollView;
    private boolean isFirstScheduleSelection = true;
    private int lastScheduleType = -1;

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        // get schedule from state helper if any
        schedule = ScheduleHelper.instance().getSchedule();

        if (schedule == null)
        {
            schedule = new Schedule();
            ScheduleHelper.instance().setSchedule(schedule);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
        Bundle savedInstanceState)
    {
        View rootView = inflater.inflate(R.layout.fragment_schedule_timetable, container, false);
        scrollView = (ScrollView) rootView.findViewById(R.id.schedule_scroll);
        timetableContainer =
            (LinearLayout) rootView.findViewById(R.id.schedule_timetable_container);
        boxHourlyInterval = rootView.findViewById(R.id.box_hourinterval);
        boxTimesByDay = rootView.findViewById(R.id.box_schedule_times_by_day);
        boxTimetable = rootView.findViewById(R.id.box_schedule_timetable);
        boxRepeat = rootView.findViewById(R.id.box_schedule_repeat);
        boxPeriod = rootView.findViewById(R.id.box_schedule_period);
        boxDuration = rootView.findViewById(R.id.box_schedule_duration);
        boxHelp = rootView.findViewById(R.id.box_schedule_help);
        helpView = (TextView) rootView.findViewById(R.id.schedule_help_text);
        nextButton = (ImageButton) rootView.findViewById(R.id.schedule_help_button);
        hourlyIntervalEditText = (Button) rootView.findViewById(R.id.hourinterval_edit_text);
        hourlyIntervalFrom = (Button) rootView.findViewById(R.id.hourinterval_from_text);
        hourlyIntervalRepeatDose = (Button) rootView.findViewById(R.id.repeat_dose);
        scheduleSpinner = (Spinner) rootView.findViewById(R.id.schedules_spinner);
        buttonScheduleStart = (Button) rootView.findViewById(R.id.button_set_start);
        buttonScheduleEnd = (Button) rootView.findViewById(R.id.button_set_end);
        clearStartButton = (ImageButton) rootView.findViewById(R.id.button_clear_start);
        clearEndButton = (ImageButton) rootView.findViewById(R.id.button_clear_end);
        repeatTypeSpinner = (Spinner) rootView.findViewById(R.id.repeat_type_spinner);

        periodValue = (Button) rootView.findViewById(R.id.period_value);
        periodRest = (Button) rootView.findViewById(R.id.period_rest);

        freqSpinner = (Spinner) rootView.findViewById(R.id.freq_spinner);
        daySelectionBox = rootView.findViewById(R.id.day_selector_box);
        customRepeatBox = rootView.findViewById(R.id.custom_repeat_box);
        intervalEditText = (Button) rootView.findViewById(R.id.interval_edit_text);
        ruleText = (TextView) rootView.findViewById(R.id.rule_text);
        cycleSpinnerTrigger = (ImageButton) rootView.findViewById(R.id.cycles_spinner_trigger);

        setupScheduleSpinner();
        setupDaySelectionListeners(rootView);
        setupHourlyRepetitionLinsteners();
        setupStartEndDatePickers(rootView);
        setupForCurrentSchedule(rootView);
        setupCycleSpinner();

        setupForCurrentSchedule(rootView);
        return rootView;
    }

    private void setupCycleSpinner()
    {
        final String[] cycles = getResources().getStringArray(R.array.schedule_cycles);

        cycleSpinnerTrigger.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v)
            {

                AlertDialog.Builder b = new AlertDialog.Builder(getActivity());
                b.setTitle(R.string.schedule_type_common_periods);
                b.setItems(cycles, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which)
                    {
                        dialog.dismiss();
                        String c = cycles[which];
                        String[] parts = c.replaceAll(" ", "").split("\\+");
                        periodValue.setText(parts[0]);
                        periodRest.setText(parts[1]);
                        schedule.setCycle(Integer.valueOf(parts[0]), Integer.valueOf(parts[1]));
                    }
                });
                b.show();
            }
        });
    }

    private void setupForCurrentSchedule(View rootView)
    {
        if (schedule.getId() != null)
        {
            setupForKnownSchedule(rootView);
        } else
        {
            setupForNewSchedule(rootView);
        }
    }

    public void onTypeSelected()
    {
        Log.d(TAG, "onTypeSelected");
        if (getView() != null)
        {
            Log.d(TAG, "getView() is not null");
            setupForCurrentSchedule(getView());
        }
    }

    private void updateHourlyIntervalBox()
    {
        LocalTime t = schedule.startTime();
        if (t == null)
        {
            t = LocalTime.now().withMinuteOfHour(0);
            schedule.setStartTime(t);
        }
        String time = new LocalTime(t.getHourOfDay(), t.getMinuteOfHour()).toString("kk:mm");
        hourlyIntervalFrom.setText(getString(R.string.first_intake) + ": " + time);

        if (schedule.rule().interval() < 1)
        {
            schedule.rule().setInterval(8);
        }
        schedule.rule().setFrequency(Frequency.HOURLY);
        hourlyIntervalEditText.setText(String.valueOf(schedule.rule().interval()));
        hourlyIntervalRepeatDose.setText(schedule.displayDose());
    }

    private void setupForKnownSchedule(View rootView)
    {

        int type = ScheduleHelper.instance().getScheduleType();
        Log.d(TAG, "Setup for known schedule:  " + type);

        boxTimesByDay.setVisibility(View.GONE);
        boxTimetable.setVisibility(View.GONE);
        boxRepeat.setVisibility(View.GONE);
        boxHourlyInterval.setVisibility(View.GONE);
        boxHelp.setVisibility(View.GONE);
        boxPeriod.setVisibility(View.GONE);
        boxDuration.setVisibility(View.VISIBLE);

        if (type == ScheduleTypeFragment.TYPE_ROUTINES)
        {
            boxTimesByDay.setVisibility(View.VISIBLE);
            boxTimetable.setVisibility(View.VISIBLE);
            boxRepeat.setVisibility(View.VISIBLE);

            timesPerDay = ScheduleHelper.instance().getTimesPerDay();
            scheduleSpinner.setSelection(ScheduleHelper.instance().getSelectedScheduleIdx());

            checkSelectedDays(rootView, schedule.days());
            setupRepetitions(rootView);
            updateRepeatTypeAndInterval(rootView);
        } else if (type == ScheduleTypeFragment.TYPE_HOURLY)
        {
            schedule.rule().setDays(Schedule.noWeekDays());
            boxHourlyInterval.setVisibility(View.VISIBLE);
            updateHourlyIntervalBox();
        } else
        {
            boxTimesByDay.setVisibility(View.VISIBLE);
            boxTimetable.setVisibility(View.VISIBLE);
            boxPeriod.setVisibility(View.VISIBLE);
            updatePeriodSelector();
        }
        boxHelp.setVisibility(View.GONE);
    }

    private void setupForNewSchedule(View rootView)
    {
        int type = ScheduleHelper.instance().getScheduleType();
        Log.d(TAG, "Setup for new schedule:  " + type);

        boxTimesByDay.setVisibility(View.GONE);
        boxTimetable.setVisibility(View.GONE);
        boxRepeat.setVisibility(View.GONE);
        boxDuration.setVisibility(View.GONE);
        boxPeriod.setVisibility(View.GONE);
        boxHourlyInterval.setVisibility(View.GONE);
        boxHelp.setVisibility(View.VISIBLE);

        if (type == ScheduleTypeFragment.TYPE_ROUTINES)
        {
            boxTimesByDay.setVisibility(View.VISIBLE);
            timesPerDay = ScheduleHelper.instance().getTimesPerDay();
            scheduleSpinner.setSelection(ScheduleHelper.instance().getSelectedScheduleIdx());

            checkSelectedDays(rootView, schedule.days());
            setupRepetitions(rootView);
            updateRepeatTypeAndInterval(rootView);
            helpView.setText(getString(R.string.schedule_help_timesbyday));

        } else if (type == ScheduleTypeFragment.TYPE_HOURLY)
        {
            schedule.setType(Schedule.SCHEDULE_TYPE_HOURLY);
            boxHourlyInterval.setVisibility(View.VISIBLE);
            schedule.setDays(Schedule.noWeekDays());
            updateHourlyIntervalBox();
            helpView.setText(getString(R.string.schedule_help_hourly));
        } else
        {
            schedule.setType(Schedule.SCHEDULE_TYPE_CYCLE);
            boxTimesByDay.setVisibility(View.VISIBLE);
            timesPerDay = ScheduleHelper.instance().getTimesPerDay();
            scheduleSpinner.setSelection(ScheduleHelper.instance().getSelectedScheduleIdx());
            helpView.setText(getString(R.string.schedule_help_timesbyday));
            updatePeriodSelector();
        }
        nextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v)
            {
                showNext();
            }
        });
    }

    private void updatePeriodSelector()
    {
        periodValue.setText(String.valueOf(schedule.getCycleDays()));
        periodRest.setText(String.valueOf(schedule.getCycleRest()));

        periodValue.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v)
            {
                NumberPickerBuilder npb =
                    new NumberPickerBuilder().setDecimalVisibility(NumberPicker.INVISIBLE)
                        .setMinNumber(1)
                        .setMaxNumber(100)
                        .setPlusMinusVisibility(NumberPicker.INVISIBLE)
                        .setFragmentManager(getChildFragmentManager())
                        .setTargetFragment(ScheduleTimetableFragment.this)
                        .setReference(REF_DIALOG_CYCLE_DAYS)
                        .setStyleResId(R.style.BetterPickersDialogFragment_Calendula);
                npb.show();
            }
        });

        periodRest.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v)
            {
                NumberPickerBuilder npb =
                    new NumberPickerBuilder().setDecimalVisibility(NumberPicker.INVISIBLE)
                        .setMinNumber(1)
                        .setMaxNumber(100)
                        .setPlusMinusVisibility(NumberPicker.INVISIBLE)
                        .setFragmentManager(getChildFragmentManager())
                        .setTargetFragment(ScheduleTimetableFragment.this)
                        .setReference(REF_DIALOG_CYCLE_REST)
                        .setStyleResId(R.style.BetterPickersDialogFragment_Calendula);
                npb.show();
            }
        });
    }

    private void updateRepeatTypeAndInterval(View rootView)
    {
        int repeatType = schedule.type();
        setRepeatType(repeatType, rootView, true);
        repeatTypeSpinner.setSelection(repeatType);
        intervalEditText.setText(String.valueOf(schedule.rule().interval()));
    }

    private void showNext()
    {
        int type = ScheduleHelper.instance().getScheduleType();

        if (type == ScheduleTypeFragment.TYPE_ROUTINES)
        {
            if (boxTimesByDay.getVisibility() != View.VISIBLE)
            {
                helpView.setText(getString(R.string.schedule_help_timesbyday));
                boxTimesByDay.setVisibility(View.VISIBLE);
                boxHourlyInterval.setVisibility(View.INVISIBLE);
            } else if (boxTimetable.getVisibility() != View.VISIBLE)
            {
                helpView.setText(getString(R.string.schedule_help_timetable));
                showBox(boxTimetable);
            } else if (boxRepeat.getVisibility() != View.VISIBLE)
            {
                helpView.setText(getString(R.string.schedule_help_repeat));
                showBox(boxRepeat);
            } else if (boxDuration.getVisibility() != View.VISIBLE)
            {
                helpView.setText(getString(R.string.schedule_help_duration));
                showBox(boxDuration);
                nextButton.setVisibility(View.GONE);
            }
        } else if (type == ScheduleTypeFragment.TYPE_HOURLY)
        {
            if (boxHourlyInterval.getVisibility() != View.VISIBLE)
            {
                boxHourlyInterval.setVisibility(View.VISIBLE);
                boxTimesByDay.setVisibility(View.INVISIBLE);
                helpView.setText(getString(R.string.schedule_help_hourly));
            } else if (boxDuration.getVisibility() != View.VISIBLE)
            {
                helpView.setText(getString(R.string.schedule_help_duration));
                showBox(boxDuration);
                nextButton.setVisibility(View.GONE);
            }
        } else if (type == ScheduleTypeFragment.TYPE_PERIOD)
        {
            if (boxTimesByDay.getVisibility() != View.VISIBLE)
            {
                helpView.setText(getString(R.string.schedule_help_timesbyday));
                boxTimesByDay.setVisibility(View.VISIBLE);
                boxHourlyInterval.setVisibility(View.INVISIBLE);
            } else if (boxTimetable.getVisibility() != View.VISIBLE)
            {
                helpView.setText(getString(R.string.schedule_help_timetable));
                showBox(boxTimetable);
            } else if (boxPeriod.getVisibility() != View.VISIBLE)
            {
                helpView.setText(getString(R.string.schedule_help_cycle));
                showBox(boxPeriod);
            } else if (boxDuration.getVisibility() != View.VISIBLE)
            {
                helpView.setText(getString(R.string.schedule_help_duration));
                showBox(boxDuration);
                nextButton.setVisibility(View.GONE);
            }
        }
    }

    private void showBox(final View v)
    {

        v.setVisibility(View.VISIBLE);
        Animation a = AnimationUtils.loadAnimation(getActivity(), R.anim.anim_scheule_box_slide_up);

        scrollView.postDelayed(new Runnable() {
            @Override
            public void run()
            {
                scrollView.smoothScrollBy(0, 1000); // ensure scroll bottom
            }
        }, 200);

        a.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation)
            {
            }

            @Override
            public void onAnimationEnd(Animation animation)
            {
            }

            @Override
            public void onAnimationRepeat(Animation animation)
            {
            }
        });
        v.startAnimation(a);
    }

    void setupHourlyRepetitionLinsteners()
    {
        hourlyIntervalEditText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v)
            {
                showHourlyPickerDIalog();
            }
        });

        hourlyIntervalFrom.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v)
            {

                DateTime time = schedule.startTime().toDateTimeToday();

                RadialTimePickerDialog timePickerDialog =
                    RadialTimePickerDialog.newInstance(ScheduleTimetableFragment.this,
                        time.getHourOfDay(), time.getMinuteOfHour(), true);
                timePickerDialog.show(getChildFragmentManager(), "111");
            }
        });

        hourlyIntervalRepeatDose.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v)
            {
                showHourlyDosePickerDialog();
            }
        });
    }

    private void setupRepetitions(final View rooView)
    {
        ruleText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v)
            {
                showRecurrencePickerDialog();
            }
        });

        repeatTypeSpinner.setAdapter(
            new ArrayAdapter<>(getActivity(), R.layout.support_simple_spinner_dropdown_item,
                getResources().getStringArray(R.array.schedule_repeat_types)));
        repeatTypeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id)
            {

                setRepeatType(position, rooView, !ignoreNextEvent);
                ignoreNextEvent = false;
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent)
            {
            }
        });

        freqSpinner.setAdapter(
            new ArrayAdapter<>(getActivity(), R.layout.support_simple_spinner_dropdown_item,
                getResources().getStringArray(R.array.schedule_repeat_frequency_units)));
        freqSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id)
            {
                setFrequency(position, rooView);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent)
            {

            }
        });

        intervalEditText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v)
            {
                showIntervalPickerDIalog();
            }
        });
    }

    void setupStartEndDatePickers(View rootView)
    {

        if (schedule.start() == null)
        {
            schedule.setStart(LocalDate.now());
        }

        final LocalDate scheduleStart = schedule.start();

        buttonScheduleStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v)
            {

                DatePickerDialog dpd =
                    new DatePickerDialog(getActivity(), new DatePickerDialog.OnDateSetListener() {
                        @Override
                        public void onDateSet(DatePicker view, int year, int monthOfYear,
                            int dayOfMonth)
                        {
                            Log.d(TAG, year + " " + monthOfYear);
                            LocalDate d = new LocalDate(year, monthOfYear + 1, dayOfMonth);
                            setScheduleStart(d);
                        }
                    }, scheduleStart.getYear(), scheduleStart.getMonthOfYear() - 1,
                        scheduleStart.getDayOfMonth());
                dpd.show();
            }
        });

        buttonScheduleEnd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v)
            {

                LocalDate scheduleEnd =
                    schedule.end() != null ? schedule.end() : scheduleStart.plusMonths(3);

                DatePickerDialog dpd =
                    new DatePickerDialog(getActivity(), new DatePickerDialog.OnDateSetListener() {
                        @Override
                        public void onDateSet(DatePicker view, int year, int monthOfYear,
                            int dayOfMonth)
                        {
                            LocalDate d = new LocalDate(year, monthOfYear + 1, dayOfMonth);
                            setScheduleEnd(d);
                        }
                    }, scheduleEnd.getYear(), scheduleEnd.getMonthOfYear() - 1,
                        scheduleEnd.getDayOfMonth());
                dpd.show();
            }
        });

        buttonScheduleEnd.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v)
            {
                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                builder.setMessage("Do you want this schedule to continue indefinitely?")
                    .setCancelable(true)
                    .setPositiveButton(getString(R.string.dialog_yes_option),
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id)
                            {
                                setScheduleEnd(null);
                            }
                        })
                    .setNegativeButton(getString(R.string.dialog_no_option),
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id)
                            {
                                dialog.cancel();
                            }
                        });
                AlertDialog alert = builder.create();
                alert.show();
                return true;
            }
        });

        clearStartButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v)
            {
                setScheduleStart(null);
            }
        });

        clearEndButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v)
            {
                setScheduleEnd(null);
            }
        });

        setScheduleStart(schedule.start());
        setScheduleEnd(schedule.end());
    }


    private void setFrequency(int freq, View rootView)
    {
        if (ScheduleHelper.instance().getScheduleType() == ScheduleTypeFragment.TYPE_ROUTINES)
        {
            Frequency frequency = FREQ[freq];
            schedule.rule().setFrequency(frequency);

            if (frequency == Frequency.WEEKLY)
            {
                int dayCount = schedule.dayCount();
                if (dayCount == 0 || dayCount == 7)
                {
                    checkToday(rootView);
                }
                daySelectionBox.setVisibility(View.VISIBLE);
            } else
            {
                schedule.rule().setDays(null);
                daySelectionBox.setVisibility(View.GONE);
            }
        }
    }

    private void setRepeatType(int type, View v, boolean updateUi)
    {
        if (ScheduleHelper.instance().getScheduleType() == ScheduleTypeFragment.TYPE_ROUTINES)
        {
            schedule.setType(type);

            if (updateUi)
            {
                //resetRule();
                if (type == Schedule.SCHEDULE_TYPE_EVERYDAY)
                {
                    daySelectionBox.setVisibility(View.VISIBLE);
                    customRepeatBox.setVisibility(View.GONE);
                    ruleText.setVisibility(View.GONE);

                    schedule.rule().setInterval(0);
                    schedule.rule().setFrequency(Frequency.DAILY);

                    checkAllDays(v);

                } else if (type == Schedule.SCHEDULE_TYPE_SOMEDAYS)
                {
                    daySelectionBox.setVisibility(View.VISIBLE);
                    customRepeatBox.setVisibility(View.GONE);
                    ruleText.setVisibility(View.GONE);

                    // Interval
                    schedule.rule().setInterval(0);
                    // frequency
                    schedule.rule().setFrequency(Frequency.DAILY);
                    // byday
                    if (schedule.dayCount() == 7 || schedule.dayCount() == 0)
                    {
                        checkToday(v);
                    } else
                    {
                        checkSelectedDays(v, schedule.days());
                    }

                } else if (type == Schedule.SCHEDULE_TYPE_INTERVAL)
                {
                    ruleText.setVisibility(View.GONE);
                    customRepeatBox.setVisibility(View.VISIBLE);

                    int interval = schedule.rule().interval();
                    if (interval < 2)
                    {
                        interval = 2;
                    }
                    schedule.rule().setInterval(interval);
                    intervalEditText.setText(String.valueOf(schedule.rule().interval()));

                    Frequency f = schedule.rule().frequency();

                    if (f.equals(Frequency.WEEKLY))
                    {
                        freqSpinner.setSelection(1);
                        setFrequency(1, v);
                    } else if (f.equals(Frequency.MONTHLY))
                    {
                        freqSpinner.setSelection(2);
                        setFrequency(2, v);
                    } else
                    {
                        freqSpinner.setSelection(0);
                        setFrequency(0, v);
                    }
                }
            }
        }
    }

    private Spanned getCurrentSchedule()
    {

        String text =
            getString(R.string.schedule_custom_rule_text, schedule.toReadableString(getActivity()));
        int count = schedule.rule().iCalRule().getCount();
        if (count > 0)
        {
            text += "<br><u>" + getString(R.string.schedules_stop_after, count) + "</u>";
        }
        return Html.fromHtml(text);
    }

    private void checkAllDays(View v)
    {
        checkSelectedDays(v, Schedule.allWeekDays());
    }

    private void checkToday(View v)
    {
        boolean[] days = Schedule.noWeekDays();
        days[LocalDate.now().getDayOfWeek() - 1] = true;
        checkSelectedDays(v, days);
    }

    public void onViewCreated(View view, Bundle savedInstanceState)
    {
        super.onViewCreated(view, savedInstanceState);
    }

    @Override
    public void onResume()
    {
        super.onResume();
    }

    private void setupScheduleSpinner()
    {
        // Create an ArrayAdapter using the string array and a default spinner layout
        ArrayAdapter<CharSequence> adapter =
            ArrayAdapter.createFromResource(getActivity(), R.array.schedules_array,
                R.layout.spinner_text_item);
        // Specify the layout to use when the list of choices appears
        adapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        // Apply the adapter to the spinner
        scheduleSpinner.setAdapter(adapter);
        scheduleSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l)
            {
                String selected = (String) adapterView.getItemAtPosition(i);
                onScheduleSelected(selected, i);
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView)
            {
            }
        });
    }

    void onScheduleSelected(String selection, int index)
    {

        ScheduleHelper.instance().setSelectedScheduleIdx(index);
        String schedules[] = getResources().getStringArray(R.array.schedules_array);

        // obtain times per day from selected schedule
        for (int i = 0; i < schedules.length; i++)
        {
            if (schedules[i].equalsIgnoreCase(selection))
            {
                timesPerDay = i + 1;
                ScheduleHelper.instance().setTimesPerDay(timesPerDay);
                break;
            }
        }
        addTimetableEntries(timesPerDay, Routine.findAll());
    }

    void setupDaySelectionListeners(final View rootView)
    {

        View.OnClickListener listener = new View.OnClickListener() {
            @Override
            public void onClick(View view)
            {

                TextView text = ((TextView) view);
                int index;
                switch (text.getId())
                {
                    case R.id.day_mo:
                        schedule.toggleSelectedDay(0);
                        index = 0;
                        break;
                    case R.id.day_tu:
                        schedule.toggleSelectedDay(1);
                        index = 1;
                        break;
                    case R.id.day_we:
                        schedule.toggleSelectedDay(2);
                        index = 2;
                        break;
                    case R.id.day_th:
                        index = 3;
                        schedule.toggleSelectedDay(3);
                        break;
                    case R.id.day_fr:
                        schedule.toggleSelectedDay(4);
                        index = 4;
                        break;
                    case R.id.day_sa:
                        schedule.toggleSelectedDay(5);
                        index = 5;
                        break;
                    case R.id.day_su:
                        schedule.toggleSelectedDay(6);
                        index = 6;
                        break;
                    default:
                        return;
                }

                boolean daySelected = schedule.days()[index];

                if (!daySelected)
                {
                    ((TextView) view).setTextAppearance(getActivity(),
                        R.style.schedule_day_unselected);
                    view.setBackgroundResource(R.drawable.dayselector_circle_unselected);
                } else
                {
                    ((TextView) view).setTextAppearance(getActivity(),
                        R.style.schedule_day_selected);
                    view.setBackgroundResource(R.drawable.dayselector_circle);
                }

                boolean allDaysSelected = schedule.allDaysSelected();

                if (schedule.type() == Schedule.SCHEDULE_TYPE_EVERYDAY && !allDaysSelected)
                {
                    setRepeatType(Schedule.SCHEDULE_TYPE_SOMEDAYS, rootView, false);
                    ignoreNextEvent = true;
                    repeatTypeSpinner.setSelection(1);
                } else if (schedule.type() == Schedule.SCHEDULE_TYPE_SOMEDAYS && allDaysSelected)
                {
                    repeatTypeSpinner.setSelection(0);
                    schedule.setType(Schedule.SCHEDULE_TYPE_EVERYDAY);
                }

                Log.d(TAG, "All days selected: " + allDaysSelected + ", repeatType: " + schedule.type());
            }
        };

        rootView.findViewById(R.id.day_mo).setOnClickListener(listener);
        rootView.findViewById(R.id.day_tu).setOnClickListener(listener);
        rootView.findViewById(R.id.day_we).setOnClickListener(listener);
        rootView.findViewById(R.id.day_th).setOnClickListener(listener);
        rootView.findViewById(R.id.day_fr).setOnClickListener(listener);
        rootView.findViewById(R.id.day_sa).setOnClickListener(listener);
        rootView.findViewById(R.id.day_su).setOnClickListener(listener);
    }

    void showIntervalPickerDIalog()
    {
        NumberPickerBuilder npb =
            new NumberPickerBuilder().setDecimalVisibility(NumberPicker.INVISIBLE)
                .setMinNumber(1)
                .setMaxNumber(31)
                .setPlusMinusVisibility(NumberPicker.INVISIBLE)
                .setFragmentManager(getChildFragmentManager())
                .setTargetFragment(this).setReference(REF_DIALOG_ROUTINE_INTERVAL)
                .setStyleResId(R.style.BetterPickersDialogFragment_Calendula);
        npb.show();
    }

    void showHourlyPickerDIalog()
    {
        /*NumberPickerBuilder npb =
            new NumberPickerBuilder().setDecimalVisibility(NumberPicker.INVISIBLE)
                .setMinNumber(1)
                .setMaxNumber(24)
                .setPlusMinusVisibility(NumberPicker.INVISIBLE)
                .setFragmentManager(getChildFragmentManager())
                .setTargetFragment(this)
                .setReference(REF_DIALOG_HOURLY_INTERVAL)
                .setStyleResId(R.style.BetterPickersDialogFragment_Calendula);
        npb.show();*/

        AlertDialog.Builder b = new AlertDialog.Builder(getActivity());
        b.setTitle(getString(R.string.dialog_interval_title));
        final String[] types = { "2", "3", "4", "6", "8", "12" };
        b.setItems(types, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which)
            {
                dialog.dismiss();
                int result = Integer.valueOf(types[which]);
                hourlyIntervalEditText.setText("" + result);
                schedule.rule().setFrequency(Frequency.HOURLY);
                schedule.rule().setInterval(result);
            }
        });

        b.show();


    }

    void showRecurrencePickerDialog()
    {
        RecurrencePickerDialog dialog = new RecurrencePickerDialog();

        DateTime start = schedule.start() != null ? schedule.start().toDateTimeAtStartOfDay()
            : DateTime.now().withTimeAtStartOfDay();

        Bundle b = new Bundle();

        b.putString(RecurrencePickerDialog.BUNDLE_RRULE,
            schedule.rule().toIcal().replace("RRULE:", ""));
        b.putLong(RecurrencePickerDialog.BUNDLE_START_TIME_MILLIS, DateTime.now().getMillis());
        //b.putString(RecurrencePickerDialog.BUNDLE_TIME_ZONE, t.timezone);

        dialog.setArguments(b);
        dialog.setOnRecurrenceSetListener(this);
        dialog.show(getChildFragmentManager(), "REC");
    }

    void addTimetableEntries(int timesPerDay, List<Routine> routines)
    {

        Collections.sort(ScheduleHelper.instance().getScheduleItems(), scheduleItemComparator);

        LinearLayout.LayoutParams params =
            new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);

        String[] routineNames = getUpdatedRoutineNames();
        timetableContainer.removeAllViews();

        List<ScheduleItem> scheduleItems = new ArrayList<ScheduleItem>();

        boolean enableDelete = timesPerDay > 1;

        for (int i = 0; i < timesPerDay; i++)
        {
            // try to get previous routine from state holder
            ScheduleItem s;

            if (i < ScheduleHelper.instance().getScheduleItems().size())
            {
                ScheduleItem toCopy = ScheduleHelper.instance().getScheduleItems().get(i);
                s = new ScheduleItem(null, toCopy.routine(), toCopy.dose());
            } else
            {
                s = new ScheduleItem(null, (i < routines.size()) ? routines.get(i) : null, 1);
            }

            if (s != null)
            {
                scheduleItems.add(s);
            }

            View view = buildTimetableEntry(s, routineNames, enableDelete);
            timetableContainer.addView(view, params);
        }

        ScheduleHelper.instance().setScheduleItems(scheduleItems);

        //        for (ScheduleItem i : ScheduleCreationHelper.instance().getScheduleItems())
        //            Log.d(TAG, "addTimetableEntries (end): " + i.getId() + ", " + i.routine().name() + ", " + i.dose());

    }

    String[] getUpdatedRoutineNames()
    {

        List<Routine> routines = Routine.findAll();

        int j = 0;
        String[] routineNames = new String[routines.size() + 1];
        for (Routine r : routines)
        {
            routineNames[j++] = r.name();
        }

        routineNames[routineNames.length - 1] = getString(R.string.create_new_routine);

        return routineNames;
    }

    View buildTimetableEntry(ScheduleItem r, String[] routineNames, boolean enableDelete)
    {
        LayoutInflater inflater = getActivity().getLayoutInflater();
        final View entry = inflater.inflate(R.layout.schedule_timetable_entry, null);
        updateEntryTime(r.routine(), entry);
        setupScheduleEntrySpinners(entry, r, routineNames);

        if (enableDelete)
        {
            entry.findViewById(R.id.entry_remove).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view)
                {
                    ScheduleHelper.instance()
                        .getScheduleItems()
                        .remove(timetableContainer.indexOfChild(entry));
                    scheduleSpinner.setSelection(timesPerDay - 2);
                }
            });
        } else
        {
            entry.findViewById(R.id.entry_remove).setVisibility(View.INVISIBLE);
        }
        return entry;
    }

    void updateRoutineSelectionAdapter(final View entryView, Spinner routineSpinner,
        String[] routineNames)
    {
        ArrayAdapter<String> routineAdapter =
            new ArrayAdapter<String>(getActivity(), android.R.layout.simple_spinner_item,
                routineNames);
        routineAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        routineSpinner.setAdapter(routineAdapter);
    }

    private void setupScheduleEntrySpinners(final View entryView, ScheduleItem scheduleItem,
        String[] routineNames)
    {

        final Spinner routineSpinner = (Spinner) entryView.findViewById(R.id.entry_routine_spinner);
        final TextView doseTv = (TextView) entryView.findViewById(R.id.entry_dose_textview);
        //        final Spinner doseSpinner = (Spinner) entryView.findViewById(R.id.entry_dose_spinner);

        doseTv.setTag(scheduleItem);
        routineSpinner.setTag(scheduleItem);

        // set up the routine selection adapter
        updateRoutineSelectionAdapter(entryView, routineSpinner, routineNames);

        if (scheduleItem != null && scheduleItem.routine() != null)
        {
            String routineName = scheduleItem.routine().name();
            int index = Arrays.asList(routineNames).indexOf(routineName);
            routineSpinner.setSelection(index);
        } else
        {
            routineSpinner.setSelection(routineNames.length - 1);
        }

        doseTv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v)
            {
                showDosePickerDialog((ScheduleItem) v.getTag(), (TextView) v);
            }
        });

        // set up the dose selection adapter
        //        ArrayAdapter<String> doseAdapter = new ArrayAdapter<String>(getActivity(), android.R.layout.simple_spinner_item, doses);
        //        doseAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        //        doseSpinner.setAdapter(doseAdapter);
        // select 1 pill by default
        //        doseSpinner.setSelection((int) scheduleItem.dose() - 1); // dose "1" is located at the index "0", and so on

        doseTv.setText(scheduleItem.displayDose());

        // setup listeners
        routineSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l)
            {
                String selected = (String) adapterView.getItemAtPosition(i);
                Routine r = Routine.findByName(selected);
                ScheduleItem item = ((ScheduleItem) routineSpinner.getTag());

                if (r != null)
                {
                    updateEntryTime(r, entryView);
                } else
                {
                    updateEntryTime(null, entryView);
                    showAddNewRoutineDialog(entryView);
                }
                Log.d(TAG, "Updated routine to "
                    + (r != null ? r.name() : "NULL")
                    + " on item "
                    + item.getId());
                item.setRoutine(r);

                logScheduleItems();
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView)
            {
            }
        });

        routineSpinner.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent)
            {
                if (motionEvent.getAction() == MotionEvent.ACTION_UP)
                {

                    if (((String) routineSpinner.getSelectedItem()).equalsIgnoreCase(
                        getString(R.string.create_new_routine)))
                    {
                        showAddNewRoutineDialog(entryView);
                        return true;
                    }
                }
                return false;
            }
        });
    }

    private void logScheduleItems()
    {
        for (ScheduleItem si : ScheduleHelper.instance().getScheduleItems())
        {
            Log.d("TAG", (si.routine() != null ? si.routine().name() : "NONE")
                + ", "
                + si.dose()
                + " ****************************");
        }
    }

    void updateEntryTime(Routine r, View entry)
    {
        String hourText;
        String minuteText;
        if (r != null)
        {
            hourText = (r.time().getHourOfDay() < 10 ? "0" + r.time().getHourOfDay()
                : r.time().getHourOfDay()) + ":";
            minuteText = (r.time().getMinuteOfHour() < 10 ? "0" + r.time().getMinuteOfHour()
                : r.time().getMinuteOfHour()) + "";
        } else
        {
            hourText = "--:";
            minuteText = "--";
        }

        ((TextView) entry.findViewById(R.id.hour_text)).setText(hourText);
        ((TextView) entry.findViewById(R.id.minute_text)).setText(minuteText);
    }

    void showAddNewRoutineDialog(final View entryView)
    {
        FragmentManager fm = getActivity().getSupportFragmentManager();
        final RoutineCreateOrEditFragment addRoutineFragment = new RoutineCreateOrEditFragment();
        addRoutineFragment.setOnRoutineEditListener(
            new RoutineCreateOrEditFragment.OnRoutineEditListener() {
                @Override
                public void onRoutineEdited(Routine r)
                {
                    // do nothing
                }

                @Override
                public void onRoutineDeleted(Routine r)
                {
                    // do nothing
                }

                @Override
                public void onRoutineCreated(final Routine r)
                {
                    Spinner rSpinner = (Spinner) entryView.findViewById(R.id.entry_routine_spinner);
                    String names[] = getUpdatedRoutineNames();
                    updateRoutineSelectionAdapter(entryView, rSpinner, names);

                    Log.d(TAG, "Routine name: " + r.name());
                    Log.d(TAG, "Routine time: " + r.time().toString("hh:mm"));
                    Log.d(TAG, "Names: " + Arrays.toString(names));

                    int selection = Arrays.asList(names).indexOf(r.name());
                    rSpinner.setSelection(selection);

                    updateEntryTime(r, entryView);
                    addRoutineFragment.dismiss();
                }
            });

        addRoutineFragment.show(fm, "fragment_edit_name");
    }

    void showDosePickerDialog(final ScheduleItem item, final TextView tv)
    {
        FragmentManager fm = getActivity().getSupportFragmentManager();
        final DosePickerFragment dosePickerFragment = new DosePickerFragment();
        Bundle arguments = new Bundle();
        arguments.putDouble("dose", item.dose());
        dosePickerFragment.setArguments(arguments);

        dosePickerFragment.setOnDoseSelectedListener(
            new DosePickerFragment.OnDoseSelectedListener() {
                @Override
                public void onDoseSelected(double dose)
                {
                    Log.d(TAG, "Set dose "
                        + dose
                        + " to item "
                        + item.routine().name()
                        + ", "
                        + item.getId());
                    item.setDose((float) dose);
                    tv.setText(item.displayDose());

                    logScheduleItems();
                }
            });

        dosePickerFragment.show(fm, "fragment_select_dose");
    }

    void showHourlyDosePickerDialog()
    {
        FragmentManager fm = getActivity().getSupportFragmentManager();
        final DosePickerFragment dosePickerFragment = new DosePickerFragment();
        Bundle arguments = new Bundle();
        arguments.putDouble("dose", schedule.dose());
        dosePickerFragment.setArguments(arguments);

        dosePickerFragment.setOnDoseSelectedListener(
            new DosePickerFragment.OnDoseSelectedListener() {
                @Override
                public void onDoseSelected(double dose)
                {

                    schedule.setDose((float) dose);
                    hourlyIntervalRepeatDose.setText(schedule.displayDose());
                }
            });
        dosePickerFragment.show(fm, "fragment_select_dose");
    }

    void checkSelectedDays(View rootView, boolean[] days)
    {

        Log.d(TAG, "Checking selected days: " + Arrays.toString(days));

        schedule.setDays(days);
        ((TextView) rootView.findViewById(R.id.day_mo)).setTextAppearance(getActivity(),
            days[0] ? R.style.schedule_day_selected : R.style.schedule_day_unselected);
        rootView.findViewById(R.id.day_mo)
            .setBackgroundResource(
                days[0] ? R.drawable.dayselector_circle : R.drawable.dayselector_circle_unselected);

        ((TextView) rootView.findViewById(R.id.day_tu)).setTextAppearance(getActivity(),
            days[1] ? R.style.schedule_day_selected : R.style.schedule_day_unselected);
        (rootView.findViewById(R.id.day_tu)).setBackgroundResource(
            days[1] ? R.drawable.dayselector_circle : R.drawable.dayselector_circle_unselected);

        ((TextView) rootView.findViewById(R.id.day_we)).setTextAppearance(getActivity(),
            days[2] ? R.style.schedule_day_selected : R.style.schedule_day_unselected);
        (rootView.findViewById(R.id.day_we)).setBackgroundResource(
            days[2] ? R.drawable.dayselector_circle : R.drawable.dayselector_circle_unselected);

        ((TextView) rootView.findViewById(R.id.day_th)).setTextAppearance(getActivity(),
            days[3] ? R.style.schedule_day_selected : R.style.schedule_day_unselected);
        (rootView.findViewById(R.id.day_th)).setBackgroundResource(
            days[3] ? R.drawable.dayselector_circle : R.drawable.dayselector_circle_unselected);

        ((TextView) rootView.findViewById(R.id.day_fr)).setTextAppearance(getActivity(),
            days[4] ? R.style.schedule_day_selected : R.style.schedule_day_unselected);
        (rootView.findViewById(R.id.day_fr)).setBackgroundResource(
            days[4] ? R.drawable.dayselector_circle : R.drawable.dayselector_circle_unselected);

        ((TextView) rootView.findViewById(R.id.day_sa)).setTextAppearance(getActivity(),
            days[5] ? R.style.schedule_day_selected : R.style.schedule_day_unselected);
        (rootView.findViewById(R.id.day_sa)).setBackgroundResource(
            days[5] ? R.drawable.dayselector_circle : R.drawable.dayselector_circle_unselected);

        ((TextView) rootView.findViewById(R.id.day_su)).setTextAppearance(getActivity(),
            days[6] ? R.style.schedule_day_selected : R.style.schedule_day_unselected);
        (rootView.findViewById(R.id.day_su)).setBackgroundResource(
            days[6] ? R.drawable.dayselector_circle : R.drawable.dayselector_circle_unselected);
    }

    @Override
    public void onDialogNumberSet(int reference, int number, double decimal, boolean isNegative,
        double fullNumber)
    {

        if (reference == REF_DIALOG_ROUTINE_INTERVAL)
        {
            intervalEditText.setText("" + number);
            schedule.rule().setInterval(number);
        } else if (reference == REF_DIALOG_HOURLY_INTERVAL)
        {
            hourlyIntervalEditText.setText("" + number);
            schedule.rule().setFrequency(Frequency.HOURLY);
            schedule.rule().setInterval(number);
        } else if (reference == REF_DIALOG_CYCLE_DAYS)
        {
            periodValue.setText(String.valueOf(number));
            cycleDays = number;
            if (cycleRest > 0)
            {
                schedule.setCycle(cycleDays, cycleRest);
            }
        } else if (reference == REF_DIALOG_CYCLE_REST)
        {
            periodRest.setText(String.valueOf(number));
            cycleRest = number;
            if (cycleDays > 0)
            {
                schedule.setCycle(cycleDays, cycleRest);
            }
        }
    }

    @Override public void onTimeSet(RadialTimePickerDialog radialTimePickerDialog, int hour,
        int minute)
    {

        String time = new LocalTime(hour, minute).toString("kk:mm");
        hourlyIntervalFrom.setText(getString(R.string.first_intake) + ": " + time);
        schedule.setStartTime(new LocalTime(hour, minute));
    }

    @Override
    public void onRecurrenceSet(String s)
    {

        EventRecurrence event = new EventRecurrence();

        LocalDate now = LocalDate.now();
        Time startDate = new Time(Time.getCurrentTimezone());
        startDate.set(now.getDayOfMonth(), now.getMonthOfYear(), now.getYear());
        startDate.normalize(true);
        event.parse(s);
        event.setStartDate(startDate);

        Log.d(TAG, "OnRecurrenceSet: " + event.startDate);

        schedule.setRepetition(new RepetitionRule("RRULE:" + s));
        setScheduleStart(schedule.start());
        LocalDate end = schedule.end();
        Log.d(TAG, "ICAL: " + schedule.rule().toIcal());
        setScheduleEnd(end);
        Log.d(TAG, "ICAL: " + schedule.rule().toIcal());
        ruleText.setText(getCurrentSchedule());
    }

    void setScheduleStart(LocalDate start)
    {
        schedule.setStart(start);
        if (start == null)
        {
            buttonScheduleStart.setText(getString(R.string.button_schedule_repeat_today));
            clearStartButton.setVisibility(View.INVISIBLE);
        } else
        {
            buttonScheduleStart.setText(
                start.toString(getString(R.string.schedule_limits_date_format)));
            clearStartButton.setVisibility(View.VISIBLE);
        }
    }

    void setScheduleEnd(LocalDate end)
    {
        if (end == null)
        {
            buttonScheduleEnd.setText(getString(R.string.never));
            schedule.rule().iCalRule().setUntil(null);
            clearEndButton.setVisibility(View.INVISIBLE);
        } else
        {
            DateValue v =
                new DateTimeValueImpl(end.getYear(), end.getMonthOfYear(), end.getDayOfMonth(), 0,
                    0, 0);
            schedule.rule().iCalRule().setUntil(v);
            buttonScheduleEnd.setText(
                end.toString(getString(R.string.schedule_limits_date_format)));
            clearEndButton.setVisibility(View.VISIBLE);
        }
    }
}