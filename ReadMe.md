# Note

This is currently All Rights Reserved as this may or may not become the basis to an official Dolthub App. I'll have to discuss the future of the app once I get the proof of concept working and in a state so it wouldn't run afoul of the Google Play Store (due to my W^X hack).

Here's an interesting link to my asking about the proper way to work with W^E: https://stackoverflow.com/questions/67021592/what-is-the-proper-way-to-embed-a-cli-executable-on-android-10-now-that-we-is

# Development Status

I may have to get someone else to help out with the memory management portion if I want to go anywhere fast with supporting very large repos. However, for now, I'm going to take a few days off of the app project so I can work on the hospital bounties.

For anyone interested, my goal is to have the app structured in a way that Android's Out Of Memory Killer won't target the app as a whole (and kill other processes like cloning other repos or other sql-servers which won't use a lot of memory). My ideas to accomplish this include, separating out the SQL Server into a process that's not linked to the app (so the OOMK won't target the app), or limit the process' memory myself. I'm considering using the `ulimit` command to perform option 2 as there doesn't seem to be a way I know of in order to accomplish option 1. The goal is not to bypass the memory killer, but is instead to keep the memory killer from taking collateral damage.

I'm using the repo, https://www.dolthub.com/repositories/post-no-preference/options, as the test repo to trigger the OOMK. My code is at https://github.com/alexis-evelyn/Dullard-Client-For-Dolthub, and is written in Java. I can provide instructions on how to compile the Dolt CLI for Android (or if you want, I can just provide the binaries directly).

You don't need root to work on the app, but if you do decide to use root to test the root portion of the app, heed the warning the app provides. The root mode is supposed to bypass the OOMK, but I haven't set up the app to write to /proc/own_pid/oom_adj yet, so it *shouldn't* cause the phone to freeze or anything. If you do decide to write to the file, it needs to be repeatedly set to -17 to bypass the OOMK and will result in other apps being closed when the phone runs out of memory.