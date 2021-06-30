#!/usr/bin/env ruby

@version = ""
@type = ""

def confirm_dir
    return false unless File.exist?("./setversion.sh")
end

def get_user_data
    puts "What version are you releasing?:"
    version = gets.chomp

    puts "Is this a GA release or an RC (if RC add RC number):"
    type = gets.chomp
end

def validate_and_store_input(version:, type:)
    return false unless version && type
    return false unless version == /^(\d+\.)?(\d+\.)?(\*|\d+)$/
    return false unless type == /^ga$/i || type == /^rc\d+$/i

    @version = version
    @type = type

    return true
end

def update_git
    puts "Checking out main and pulling latest changes..."
    system('git checkout main')
    system('git pull')
    branch_exists? = system("git checkout -b release/", @version)
    system("git checkout release/", @version) if branch_exists?
end

def run_setversion
    system("./setversion.sh", @version, "1", @type)
end

def commit_new_version
    system('git add version.properties')
    system("git commit -m 'Release ", @version, "'")
    system("git tag -a v", @version, " -m 'Release ", @version, "'")
    system("git push -u origin release/", @version)
    system("git push origin v", @version)
end