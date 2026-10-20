package com.katchy.focuslive.ui.planner

// Legacy course/type links still identify deliveries even before kind was persisted.
internal fun isAgendaTask(kind: String, courseId: String?, deliverableType: String?): Boolean =
    kind == "AGENDA" && courseId.isNullOrBlank() && deliverableType.isNullOrBlank()
