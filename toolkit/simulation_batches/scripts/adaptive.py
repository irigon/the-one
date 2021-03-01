# create configurations based on filename and run one.sh on the create configuration

import os, shutil
from configparser import ConfigParser
from os import path
from optparse import OptionParser
from subprocess import call
import script_tools as st
from pathlib import Path


# The goal of this script is to automate the simulations in the-ONE

print('Start adaptive.py: for split and unsplitted scenarios')

config = ConfigParser()
config.read('defaults.cfg')


parser = OptionParser()
parser.add_option("-i", "--input", dest="opt_input",
                  help="options file")
parser.add_option("-d", "--defaults", dest="defaults_file",
                  help="configuration file")
parser.add_option("-s", "--scenario", dest="scenario_desc", type=str,
                  help="scenario description")
parser.add_option("--dry-run", action='store_true', dest="dry_run")


(options, args) = parser.parse_args()

THE_ONE_PATH = config.get('path', 'the_one_path')
THE_ONE_SCRIPTS = THE_ONE_PATH + "toolkit/simulation_batches/"
SETTINGS_DIR = THE_ONE_SCRIPTS + "settings/"
DEFAULTS = SETTINGS_DIR + options.defaults_file
CONFIGURATION_PATH = SETTINGS_DIR + options.opt_input
DEFAULT_SETTINGS_FILE = THE_ONE_PATH + "default_settings.txt"
SCENARIO_LIST=options.scenario_desc.split(',')

def backup_configuration_file(default_config, config_name):
    os.chdir(THE_ONE_PATH)
    Path("reports_configuration").mkdir(parents=True, exist_ok=True)
    shutil.copyfile(default_config, 'reports_configuration/' + config_name)

# router name, bundle size, simulation time, event interval, beta, gamma, alternative mobility speed, waiting time, number of alternative mobiles, communication window, adaptivo?

def get_template():
    return "{}_router:{}_EventSize:{}_endTime:{}_Events1.interval:{}_beta:{}_gamma:{}"

os.chdir(THE_ONE_PATH)
dst_config = SETTINGS_DIR + "tmp.txt"

# get dict from config file
dic = st.read_config(CONFIGURATION_PATH)

# return a list of dicts from the cartesian product of dic values
dict_list = st.product(dic)


dict_list = [dict(t) for t in {tuple(d.items()) for d in dict_list}]

''' Create the output file name
'''
def get_end_name(scenario_name, variable_list, fn):
    list_of_values = [scenario_name]
    list_of_values.extend([fn(x) for x in variable_list])
    template = get_template()
    return template.format(*list_of_values)

total   = len(dict_list)
for scenario in SCENARIO_LIST:
    counter = 0
    # for each dic (item of the cartesian product), i.e., for each possible configuration
    for entry in dict_list:
        counter += 1
        scenario_name = scenario.split('_')[0]

        # function that gets the respective entry for a name
        variable_list = ['Group.router','Events1.size','Scenario.endTime','Events1.interval','PTWRouter.beta', 'PTWRouter.gamma']
        #variable_list = ['Group.router','Events1.size','Scenario.endTime','Events1.interval','ProphetV2Router.beta', 'ProphetV2Router.gamma']
        name_from_dict = lambda x: entry[x]

        template = get_template()

        # end_name: name of the file in reports
        list_of_values = [scenario_name]
        list_of_values.extend([name_from_dict(x) for x in variable_list])
        end_name = template.format(*list_of_values)

        # Config name --> format of Scenario.name in default_settings.
        ONE_name = lambda x: '%%{}%%'.format(x)
        scen_config_name = template.format(scenario_name, *[ONE_name(x) for x in variable_list])
        scen_config_name = scen_config_name.format('')


        report_name = THE_ONE_PATH + "reports/" + end_name + "_MessageStatsReport.txt"
        if path.exists(report_name):
            print("Ignoring existing simulation:  {}".format(report_name))
            continue
        else:
            print(report_name + " does not exist. Simulating...")

        if options.dry_run:
            print("Dry-run concluded, exiting...")
            continue

        # touch: create the status file empty to let another process to know that this has been taken
        open(report_name, 'a').close()

        # copia para default_settings
        shutil.copyfile(DEFAULTS, DEFAULT_SETTINGS_FILE)
        st.setValues(DEFAULT_SETTINGS_FILE, "Scenario.name", scen_config_name)

        print("Setting values: {}".format(entry))
        for k, v in entry.items():
            st.setValues(DEFAULT_SETTINGS_FILE, k, v)
        print('{}'.format(["./one.sh", "-b", "1", scenario]))

        # backup configuration files
        backup_configuration_file(DEFAULT_SETTINGS_FILE, end_name + "_default_settings")
        
        print("Percent: {}/{}".format(counter,total))
        call(["./one.sh", "-b", "1", scenario])

