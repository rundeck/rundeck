//
//
quartz {
    autoStartup = true
    jdbcStore = false
}

environments {
    test {
        quartz {
            autoStartup = false
        }
    }
}