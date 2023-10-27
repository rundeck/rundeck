package rundeck.services.asyncimport

class AsyncImportMessages {

    private def static asyncImportWaitingEmojis = ["🕛","🕒","🕕","🕘"]
    def static inProcess = "🏃"
    def static check = "✅"
    def static project = "📦"
    def static importing = "☁️"
    def static save = "💾"
    def static creating = "🔨"
    def static cleaning = "🧹"
    def static done = "🎉"

    static def waitingAnimationInLogs(){
        return asyncImportWaitingEmojis[new Random().nextInt(4)]
    }

}
