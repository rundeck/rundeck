#!/usr/bin/env ruby

class AutoRelease
    GH_URLS  = {
        "rundeck": "https://github.com/rundeck/rundeck",
        "rundeckpro": "https://github.com/rundeckpro/rundeckpro",
        "auto_release_test": "https://github.com/rundeck/auto_release_test"
    }
    
    def initialize(mode: ARGV[0])
        sanitized_arg = mode.match(/^(--)?(.*)$/i).captures

        @app_url = nil
        @version = nil
        @rc = nil
        @full_version = nil
        @release_tag = nil
        @release_branch = nil
        @mode = sanitized_arg[1].downcase || "live"
        @debug = @mode == "test" ? true : false
    end

    def power_on_self_test
        current_dir = File.basename(Dir.pwd).to_sym
        pass_count = 0

        puts "---Auto-Releaser Power On Self Test---"
        case @mode
        when "test"
            puts "✓ TEST MODE ACTIVE"
            @mode = "auto_release_test"
        when "help", "noop", "live"
            puts "✓ Valid mode selected"
        else
            puts @mode
            puts "Invalid mode selected! Please try again using a valid argument. Launch using --help for details."
            user_exit(1)
        end

        if GH_URLS.key?(current_dir.downcase.to_sym)
            pass_count += 1
            puts "✓ In a supported directory (#{current_dir})"
        end

        if File.exist?("./setversion.sh")
            pass_count += 1
            puts "✓ setversion.sh exists"
        end

        if @mode == "auto_release_test" || system("git diff --exit-code")
            pass_count += 1
            puts "✓ No uncommitted changes"
        end

        if pass_count >= 3
            @app_url = GH_URLS[current_dir]
            puts "Mode is #{@mode}"
            puts "Startup check complete!"
        else
            puts "Startup check failed! Please ensure you are running in the main directory of rundeck or rundeckpro and" +
            "you have no uncommitted changes ('git stash'), and try again."
            user_exit(1)
        end
    end

    def help
        puts <<~TEXT
        Intro:
        This is a small utility to automate the manual portions of cutting a GA or RC release of Rundeck.
        It is written off of the documented instructions at https://pagerduty.atlassian.net/wiki/spaces/RUNDECK/pages/2304901608/Rundeck+Enterprise+Release+Process.
        The script is verbose on purpose. It will step you through each area of input, vocalize what it is doing, and allow you to quit one last time before changes actually occur.

        Prerequisites:
        * Any version of Ruby 2 or greater should be on your machine.
        * You should be on a clean copy of main, with no uncommitted changes.
        * The setversion.sh script must be present
        * You will need to be in the main directory of rundeck or rundeckpro

        Usage:
        "./auto_release.rb --noop" - Steps you through the prompts and flow of the script and shows you the commands that are run, but makes no actual changes.
        "./auto_release.rb --help" - Brings up this dialog.
        "./auto_release.rb --test" - Will make changes to a test repo when run from there ('git clone git@github.com:rundeck/auto_release_test.git').
        TEXT
    end

    def run
        power_on_self_test unless @mode == "help"
        header
        if @mode == "help"
            help
            user_exit
        end
        get_user_data
        progress_check
        prepare_release
        run_setversion
        commit_new_version
        wrap_up
    end


    def get_user_data
        puts "Will this be a release candidate or GA release (r, g)?"
        handle_input(prompt_type: "release_type", input: STDIN.gets.chomp)

        if @rc
            puts "What will be the RC Number?"
            @rc = "RC" + STDIN.gets.chomp
        end

        puts "What will be the version number for the release?:"
        handle_input(prompt_type: "release_version", input: STDIN.gets.chomp)
        prep_git_details
    end

    def progress_check
        puts <<~TEXT
            You're ready to go with the following details:
            Version: #{@full_version}
            Release Tag: #{@release_tag}
            Release Branch: #{@release_branch}
        TEXT

        puts "The script will now setup the release branch and tag, and push it to GitHub. Are you ready to continue? (y/n)"
        handle_input(prompt_type: "boolean", input: STDIN.gets.chomp)
    end

    def prepare_release
        puts "Checking out main and pulling latest changes..."
        system_overlord(['git', 'checkout', 'main'])
        system_overlord(['git', 'pull'])
        branch_exists = system_overlord(['git', 'checkout', '-b', "release/#{@version}"], ignore_errors: true)
        system_overlord(['git', 'checkout', "release/#{@version}"]) if branch_exists
        system_overlord(['git', 'merge', 'main'])
    end

    def run_setversion
        puts "Running ./setversion.sh and confirming changes..."
        type = @rc || "GA"
        system_overlord(['./setversion.sh', @version, type])
        version_file = IO.read("./version.properties")

        puts <<~TEXT
            This is how we've set version.properties based on your input:

            ===./version.properties===
            #{version_file}
            ===./version.properties===

            Ready to proceed with pushing this version to GitHub? (y/n)
        TEXT
        handle_input(prompt_type: "boolean", input: STDIN.gets.chomp)
    end

    def commit_new_version
        puts "Committing changes and pushing tag and branch to origin..."
        system_overlord(['git', 'add', 'version.properties'])
        system_overlord(['git', 'commit', '-m', "Release #{@version}"])
        system_overlord(['git', 'tag', '-a', "v#{@version}", '-m', "Release #{@version}"])
        system_overlord(['git', 'push', '-u', 'origin', "release/#{@version}"])
        system_overlord(['git', 'push', 'origin', "v#{@version}"])
    end

    def wrap_up
        puts <<~TEXT
            DONE!

            At this point, your code should be correctly released and available on GitHub!
            You can verify by visiting your release: #{@app_url}/tree/#{@release_branch}

        TEXT
        user_exit
    end

    private

    def system_overlord(command, ignore_errors: false)
        if @mode == "noop"
            puts "Skipping command '#{command}' since noop flag is enabled"
            return
        end

        debug("system_overlord > command: #{command}")

        if command_status = system(*command)
            puts "Ran '#{command}'"
        else
            if ignore_errors
                return false
            else
                fail_out("running '#{command}'")
            end
        end
    end

    def header
        puts "\n---Auto-Releaser for Rundeck/Rundeck Pro---"
        case @mode
        when "noop"
            puts "NOOP SESSION: No changes will be made to the system or GitHub."
        when "live"
            puts "LIVE SESSION: All changes will be saved to the system and committed to GitHub!"
        when "help"
            ""
        end
        puts "Ctrl-C to quit at any time.\n\n"
    end

    def handle_input(prompt_type:, input:)
        case prompt_type
        when "boolean"
            boolean_handler(input)
        when "release_type"
            type_handler(input)
        when "release_version"
            debug("handle_input > release_version > input: #{input}") 
            version_handler(input)
        end
    end

    def type_handler(input)
        puts "type_handler: #{input}"
        case input
        when /^g(a)?$/i
            true
        when /^r(c)?(\d+)?$/i
            @rc = "RC"
        else
            fail_out("validating type")
        end
    end

    def version_handler(input)
        fail_out("validating version") unless /^(\d+\.)?(\d+\.)?(\*|\d+)$/.match?(input)
        @version = input
    end

    def boolean_handler(input)
        case input
        when "y"
            true
        when "n"
            puts "Ok, aborting... No changes to Git were made"
            exit(0)
        else
            fail_out("validating confirmation")
        end
    end

    def prep_git_details
        @full_version = @version
        @full_version += "-#{@rc}" if @rc

        @release_tag = "v#{@full_version}"
        @release_branch = "release/#{@full_version}"
    end

    def user_exit(exit_code = 0)
        puts "Press any key to exit..."
        STDIN.getc
        exit(exit_code)
    end

    def fail_out(message)
        abort("There was an issue #{message}! Please try again. Aborting...")
    end

    def debug(message)
        puts "DEBUG> #{message}" if @debug
    end
end

AutoRelease.new.run
