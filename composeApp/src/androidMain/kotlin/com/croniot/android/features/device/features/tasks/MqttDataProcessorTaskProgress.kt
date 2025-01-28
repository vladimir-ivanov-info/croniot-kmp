    package com.croniot.android.features.device.features.tasks

    import croniot.messages.MessageFactory
    import croniot.models.MqttDataProcessor
    import croniot.models.dto.TaskStateInfoDto
    import org.koin.core.component.KoinComponent
    import org.koin.core.component.get

    class MqttDataProcessorTaskProgress() : MqttDataProcessor, KoinComponent {

        private val viewModelTasks: ViewModelTasks = get()

        override fun process(data: Any) {
            val dataString = data as String
            val taskStateInfoDto = MessageFactory.fromJsonWithZonedDateTime<TaskStateInfoDto>(dataString)
            viewModelTasks.updateTaskProgress(taskStateInfoDto)
        }
    }