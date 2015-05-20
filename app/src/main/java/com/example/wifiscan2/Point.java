package com.example.wifiscan2;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamField;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;

public class Point implements Serializable {
     // 保存某个点的特征AP
	HashMap<String, AP> aps = new HashMap<String, AP>();
	int x;
	int y;
}
