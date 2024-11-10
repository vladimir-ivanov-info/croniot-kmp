package com.croniot.android.presentation.device.taskTypes

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.croniot.android.ui.UtilUi
import androidx.compose.material3.Icon
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.croniot.android.Global
import com.croniot.android.UiConstants
import com.croniot.android.ui.task.ViewModelTasks
import com.croniot.android.ui.util.GenericDialog
import croniot.models.Result
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskTypeScreen(navController: NavController) {

    LaunchedEffect(Unit) {
        if(Global.selectedTaskType == null){
            if(!navController.popBackStack()){
                navController.navigate(UiConstants.ROUTE_DEVICE)
            }
        } else {
            println()
        }
    }

    val snackbarHostState = remember { SnackbarHostState() }

    BackHandler {
        if(!navController.popBackStack()){
            navController.navigate(UiConstants.ROUTE_DEVICE)
        }
    }


    Scaffold(
        snackbarHost = {
            SnackbarHost(snackbarHostState)
        },
        topBar = {
            TopAppBar( //This material API is experimental and is likely to change or to be removed in the future.
                title = {
                    Row(modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Start,
                        verticalAlignment = Alignment.CenterVertically) {
                        //Text("IoT client app")
                        IconButton(
                            modifier = Modifier.padding(end = 8.dp),
                            onClick = {
                                navController.popBackStack()
                                if(!navController.popBackStack()){
                                    navController.navigate(UiConstants.ROUTE_DEVICE)
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                // Add some space between the icon and the text
                            )
                        }

                        if(Global.selectedTaskType != null){
                            Box(contentAlignment = Alignment.CenterStart) {
                                Text(text = Global.selectedTaskType!!.name)
                            }
                        }


                    }
                },
                modifier = Modifier.background(MaterialTheme.colorScheme.background),
            )
        },
        content = {
                innerPadding ->
            TaskScreenContent(navController, innerPadding, snackbarHostState)
        }
    )
}

@Composable
fun TaskScreenContent(navController: NavController, innerPadding: PaddingValues, snackbarHostState: SnackbarHostState){
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
    ){
        TaskConfiguration(navController, snackbarHostState)
    }
}

@Composable
fun TaskConfiguration(navController: NavController, snackbarHostState: SnackbarHostState){

    val coroutineScope = rememberCoroutineScope()
    var showDialog by remember{ mutableStateOf(false) }
    var postNewTaskResult  by remember{ mutableStateOf(Result(false, "")) }

    val viewModelTaskTypes: ViewModelTaskTypes = viewModel()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ){
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.CenterStart,

            ){
            Text(text = "Configuration",
                fontSize = UtilUi.TEXT_SIZE_1,
                color = Color.Black
            )
        }
        Spacer(modifier = Modifier.padding(8.dp))



        LazyColumn(
            modifier = Modifier.weight(1f),
        ) {
            items(viewModelTaskTypes.parametersValues.size) { index ->

                val viewModelTasks : ViewModelTasks = koinViewModel() //TODO see why putting this before LazyColumn results in nullpointer
                val tasks by viewModelTasks.tasks.collectAsState()

                val currentParameter = viewModelTaskTypes.parametersValues.toList()[index].first
                val taskDtoFlow = tasks
                    .filter { it.value.taskUid == Global.selectedTaskType!!.uid }
                    .maxByOrNull { it.value.getLastState().dateTime }
                    ?.collectAsState()

                val taskDto = taskDtoFlow?.value

                if(currentParameter.type == "number"){

                    val minValue = currentParameter.constraints["minValue"]
                    val maxValue = currentParameter.constraints["maxValue"]

                    val sliderValue = viewModelTaskTypes.parametersValues.toList()[index].second.collectAsState()
                    var sliderValueStr = sliderValue.value

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                        ,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        val steps = maxValue!!.toInt()

                        sliderValueStr = BigDecimal(sliderValueStr.toDouble()).setScale(1, RoundingMode.HALF_UP).toString()

                        LabeledSlider(
                            parameter = currentParameter,
                            value = sliderValueStr.toFloat(),
                            onValueChange = {
                                viewModelTaskTypes.updateParameter(currentParameter.uid, it.toString())
                            },
                            valueRange = minValue!!.toFloat()..maxValue!!.toFloat(),
                            steps = steps,
                            minValue = minValue!!.toFloat(),
                            maxValue = maxValue!!.toFloat(),
                            minValueLabel = minValue,
                            maxValueLabel = maxValue,
                            currentValueLabel = "abc",
                            constraints = currentParameter.constraints
                        )
                    }
                } else if(currentParameter.type == "time"){
                    TimePicker(currentParameter, viewModelTaskTypes)
                } else if(currentParameter.type == "stateful"){
                    StatefulParameter(currentParameter, taskDto!!, viewModelTaskTypes)
                }
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            }
        }

        //TODO instead of checking first element, check if all elements are stateful or if task is immediate
        if(viewModelTaskTypes.parametersValues.toList().size == 1 && viewModelTaskTypes.parametersValues.toList()[0].first.type == "stateful"){
            //Stateful parameters don't need the Add task button to be clicked, they run on click
        } else {
            Box(modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = {
                        //TODO add confirmation dialog
                        //TODO go back?
                        coroutineScope.launch {
                            postNewTaskResult = viewModelTaskTypes.sendTask()
                            if(postNewTaskResult.success){
                                snackbarHostState.showSnackbar(
                                    message = "Task created successfully.",
                                    actionLabel = "Dismiss",
                                    duration = SnackbarDuration.Short
                                )
                            }
                        }
                    },
                    modifier = Modifier
                        .align(Alignment.Center)
                        .fillMaxWidth()
                        .padding(8.dp)
                ) {
                    Text(text = "Add task")
                }
            }
            if(showDialog){
                GenericDialog(
                    title = "New task",
                    text = postNewTaskResult.message,
                    button1Text = "Accept",
                    onButton1Clicked = { showDialog = false},
                    button2Text = "Go to Tasks",
                    onButton2Clicked = {
                        //TODO not active for now viewModelDeviceScreen.updateCurrentTab(2) //TODO make enum
                    }) {}
            }
        }

    }
}

fun formatNumber(value: Float, constraints: MutableMap<String, String>) : String{
    var result: String = value.toString()

    val decimals = constraints.get("decimals")

    if(decimals != null){
        if(decimals.toInt() == 0){
            result = value.roundToInt().toString()
        }
    }

    return result
}

