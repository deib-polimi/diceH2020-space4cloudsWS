param nAM integer >=0;
set AM := 1..nAM;
set H {AM};


param bigC {i in AM, H[i]} >=0;
param nu {i in AM, H[i]} integer >=0;
param Mtilde {i in AM, H[i]} >=0;
param Vtilde {i in AM, H[i]} >=0;
param M >=0;
param V >=0;
param N integer >=0 default 1;

### Variables
var x {i in AM, H[i]} logical;
var rej;

### Objectives
minimize knapsack_obj: sum {i in AM, j in H[i]} (bigC[i, j] * x[i, j]);

s.t. one_x {i in AM}: sum {j in H[i]} x[i, j] = 1;
s.t. RAM: sum {i in AM, j in H[i]} (Mtilde[i, j] * nu[i, j] * x[i, j]) <= M * N;
s.t. CPU: sum {i in AM, j in H[i]} (Vtilde[i, j] * nu[i, j] * x[i, j]) <= V * N;

solve;
param solfile symbolic default "solution.sol";
printf "### Saved solution ###\n" > (solfile);
printf "### Knapsack problem ###\n\n" >> (solfile);

param message := if (exists{i in AM, j in H[i]} x[i,j] >= 1) then "solved" else "";

printf "solve_result = %s\n\n", if (exists{i in AM, j in H[i]} x[i,j] >= 1) then "solved" else "" >> (solfile);

printf "### Objective\n" >> (solfile);
printf "knapsack_obj = %d\n\n", knapsack_obj >> (solfile);


printf "### Variables\n" >> (solfile);
printf "x :=\n" >> (solfile);
printf{i in AM, j in H[i]} "%s %s %d\n", i, j, x[i, j] >> (solfile);
printf ";\n\n" >> (solfile);

printf "### Rejections\n" >> (solfile);
printf "var: rej = %d\n\n", sum{i in AM} (max{j in H[i]} j - sum {j in H[i]} (j * x[i, j])) >> (solfile);

printf "### cores\n" >> (solfile);
printf "var: cores:= %.5f\n\n", sum{i in AM, j in H[i]} nu[i,j]*x[i,j]*Vtilde[i,j] >> (solfile);

printf "### Concurrency\n" >> (solfile);
printf "param: h :=\n" >> (solfile);
for {i in AM}
  {
    printf "%d %d\n", i, sum {j in H[i]} (j * x[i, j]) >> (solfile);
  }
printf ";\n\n" >> (solfile);
