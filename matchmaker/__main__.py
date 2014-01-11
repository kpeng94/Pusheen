import os
import sys
import shutil

def loadBase():
    f = open('matchmaker/base.conf')
    s = f.read()
    f.close()
    return s

DEFAULT_MATCH_FILE = 'matchmaker/matches'
BASE = loadBase()

def resetDir(f):
    f = 'matchmaker/' + f
    if os.path.exists(f):
        shutil.rmtree(f)
    os.mkdir(f)

def clean():
    resetDir('replays')
    resetDir('logs')

def genConf(mapname, a, b, logname):
    conf = BASE % (mapname, a, b, 'matchmaker/replays/%s.rms' % logname)
    f = open('matchmaker/temp.conf','w')
    f.write(conf)
    f.close()

def runMatch(mapname, a, b, logname, num):
    for i in xrange(num):
        print "Currently running match %s between %s and %s on map %s" % (i, a, b, mapname)
        logi = logname + str(i)
        genConf(mapname, a, b, logi)
        os.system('ant -f matchmaker/build.xml -Dconf=matchmaker/temp.conf > matchmaker/logs/%s.txt' % logi)
    os.remove('matchmaker/temp.conf')

def main():
    if len(sys.argv) > 2:
        print "Too many arguments"
        sys.exit(0)

    matchfile = DEFAULT_MATCH_FILE if len(sys.argv) == 1 else sys.argv[1]
    matches = open(matchfile)

    a = []
    b = []
    mapname = []
    num = []

    for line in matches:
        if not line.strip():
            continue
        line = line.split('|')
        a.append(line[0])
        b.append(line[1])
        mapname.append(line[2])
        num.append(int(line[3]))
    matches.close()

    for i in xrange(len(a)):
        print "Running %s matches between %s and %s on map %s" % (num[i], a[i], b[i], mapname[i])
        runMatch(mapname[i], a[i], b[i], '%s_vs_%s_%s' % (a[i], b[i], mapname[i]), num[i])

clean()
main()