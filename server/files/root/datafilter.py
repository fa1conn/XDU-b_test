import idc


start = idc.AskAddr(ScreenEA(),"Start Address:")
length = idc.AskLong(ItemSize(ScreenEA()),"Length:")
datatype = idc.AskStr("b","Type:")

i = 1

if datatype == "B" or datatype == "b":
    func = idc.Byte
elif datatype == "w" or datatype == "W":
    func = idc.Word
    i = 2
elif datatype == "d" or datatype == "D":
    func = idc.Dword
    i = 4
elif datatype == "q" or datatype == "Q":
    func = idc.Qword
    i = 8
elif datatype == "f" or datatype == "F":
    func = idc.GetFloat
    i = 4
elif datatype == "lf" or datatype == "LF":
    func = idc.GetDouble
    i = 8
else:
    func = idc.Byte

a = []
for n in range(0,length*i,i):
	a.append(func(start+n))
print("Totally Convert %d Bytes"%(len(a)))

print "[",
if func != idc.GetFloat and func != idc.GetDouble:
    tmp = [hex(x) for x in a]
else:
    tmp = a
for i in range(len(tmp)-1):
    print "%s,"%tmp[i],
print "%s"%tmp[len(tmp)-1],
print "]"