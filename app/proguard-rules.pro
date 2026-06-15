# Keep Gemini AI response data classes
-keepclassmembers class com.example.budgetmanager.data.sms.GeminiTransactionResponse {
    <fields>;
}
-keep class com.example.budgetmanager.data.sms.GeminiTransactionResponse

# Keep Kotlinx Serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** {
    *** serializer(...);
}
